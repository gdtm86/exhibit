/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.exhibit.etl.tbl;

import com.cloudera.exhibit.core.Obs;
import com.cloudera.exhibit.core.ObsDescriptor;
import com.cloudera.exhibit.etl.SchemaProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.List;
import java.util.Map;

public class PercentileTbl implements Tbl {

  private static final Schema DOUBLE = Schema.create(Schema.Type.DOUBLE);

  private final String obsKey;
  private final String outKey;
  private final List<Integer> percentiles;
  private int binCount;

  private Schema intermediate;
  private Schema output;
  private NumericHistogram hist;

  public PercentileTbl(Map<String, String> values, Map<String, Object> options) {
    if (values.size() != 1) {
      throw new IllegalArgumentException("PERCENTILE must have exactly one input value");
    }
    if (options.get("percentiles") == null || !(options.get("percentile") instanceof List)) {
      throw new IllegalArgumentException("PERCENTILE must have a numeric list called percentiles in its options");
    }
    Map.Entry<String, String> e = Iterables.getOnlyElement(values.entrySet());
    this.obsKey = e.getKey();
    this.outKey = e.getValue();
    this.percentiles = (List<Integer>) options.get("percentiles");
    this.binCount = options.containsKey("bins") ? (Integer) options.get("bins") : 10000;
  }

  @Override
  public SchemaProvider getSchemas(ObsDescriptor od, int outputId, int aggIdx) {
    List<Schema.Field> interFields = Lists.newArrayList();

    List<Schema.Field> outerFields = Lists.newArrayList();
    for (int p : percentiles) {
      String name = outKey + "_" + p;
      outerFields.add(new Schema.Field(name, DOUBLE, "", null));
    }
    this.output = Schema.createRecord("ExQuantile_" + outputId + "_" + aggIdx, "", "exhibit", false);
    this.output.setFields(outerFields);
    return new SchemaProvider(ImmutableList.of(intermediate, output));
  }

  @Override
  public void initialize(SchemaProvider provider) {
    this.intermediate = provider.get(0);
    this.output = provider.get(1);
    this.hist = new NumericHistogram();
    this.hist.allocate(binCount);
  }

  @Override
  public void add(Obs obs) {
    Double d = obs.get(obsKey, Double.class);
    if (d != null) {
      hist.add(d);
    }
  }

  @Override
  public GenericData.Record getValue() {
    GenericData.Record r = new GenericData.Record(intermediate);
    hist.serialize(r);
    return r;
  }

  @Override
  public GenericData.Record merge(GenericData.Record current, GenericData.Record next) {
    if (current == null) {
      return next;
    }
    NumericHistogram nh = new NumericHistogram();
    nh.allocate(binCount);
    nh.merge(current);
    nh.merge(next);
    nh.serialize(current);
    return current;
  }

  @Override
  public GenericData.Record finalize(GenericData.Record value) {
    NumericHistogram h = new NumericHistogram();
    h.allocate(binCount);
    h.merge(value);

    GenericData.Record res = new GenericData.Record(output);
    for (int p : percentiles) {
      double pv = h.quantile(p / 100.0);
      res.put(outKey + "_" + p, pv);
    }
    return res;
  }
}