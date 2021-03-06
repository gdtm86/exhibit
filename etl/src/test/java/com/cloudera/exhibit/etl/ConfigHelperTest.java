/*
 * Copyright (c) 2015, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.exhibit.etl;

import com.cloudera.exhibit.etl.config.ComputeConfig;
import com.cloudera.exhibit.etl.config.ConfigHelper;
import org.junit.Test;

public class ConfigHelperTest {

  private String testCfg = null;

  @Test
  public void testHelper() throws Exception {
    //TODO: replace w/resource file
    if (testCfg != null) {
      ComputeConfig cc = ConfigHelper.parseComputeConfig(testCfg);
      System.out.println("Success");
    }
  }
}
