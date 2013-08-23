/*
 * Copyright (C) 2013 Jerzy Chalupski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.getbase.forger.tests;

import static org.fest.assertions.Assertions.assertThat;

import com.getbase.forger.Forger;

import org.chalup.microorm.MicroOrm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FieldOverrideTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<TestModels.TestModel>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test
  public void shouldAllowOverridingFieldsInSimpleObjects() throws Exception {
    String email = "test@getbase.com";
    TestModels.User user = mTestSubject
        .iNeed(TestModels.User.class)
        .with("email", email)
        .in(mContentResolver);

    assertThat(user).isNotNull();
    assertThat(user.email).isEqualTo(email);
  }

  @Test
  public void shouldAllowOverridingFieldsWithNull() throws Exception {
    TestModels.Deal deal = mTestSubject
        .iNeed(TestModels.Deal.class)
        .with("name", null)
        .in(mContentResolver);

    assertThat(deal).isNotNull();
    assertThat(deal.name).isNull();
  }

  @Test
  public void shouldAllowOverridingBooleanFields() throws Exception {
    TestModels.User user = mTestSubject
        .iNeed(TestModels.User.class)
        .with("is_admin", true)
        .in(mContentResolver);

    assertThat(user).isNotNull();
    assertThat(user.admin).isTrue();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowOverridingPrimitiveFieldsWithNull() throws Exception {
    TestModels.Deal deal = mTestSubject
        .iNeed(TestModels.Deal.class)
        .with("user_id", null)
        .in(mContentResolver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowOverridingReadonlyFields() throws Exception {
    mTestSubject
        .iNeed(TestModels.Deal.class)
        .with("_id", 1500);
  }
}
