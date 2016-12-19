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

package com.getbase.android.forger.tests;

import static org.fest.assertions.api.Assertions.assertThat;

import com.getbase.android.forger.Forger;
import com.getbase.android.forger.KotlinDataClass;

import org.chalup.microorm.MicroOrm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BasicFakingTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  public static class ClassOutsideOfTheModelGraph {
  }

  @Test(expected = NullPointerException.class)
  public void shouldNotAllowFakingClassesOutsideOfModelGraph() throws Exception {
    mTestSubject.iNeed(ClassOutsideOfTheModelGraph.class).in(mContentResolver);
  }

  @Test
  public void shouldCreateClassWithoutDefaultConstructor() throws Exception {
    TestModels.ClassWithoutDefaultConstructor object = mTestSubject.iNeed(TestModels.ClassWithoutDefaultConstructor.class).in(mContentResolver);

    assertThat(object).isNotNull();
  }

  @Test
  public void shouldCreateClassWithoutPublicDefaultConstructor() throws Exception {
    TestModels.ClassWithoutPublicDefaultConstructor object = mTestSubject.iNeed(TestModels.ClassWithoutPublicDefaultConstructor.class).in(mContentResolver);

    assertThat(object).isNotNull();
  }

  @Test
  public void shouldCreateSimpleObject() throws Exception {
    TestModels.User user = mTestSubject.iNeed(TestModels.User.class).in(mContentResolver);

    assertThat(user).isNotNull();

    assertThat(user.id).isNotEqualTo(0);
    assertThat(user.email).isNotNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectIncorrectParentObject() throws Exception {
    mTestSubject.iNeed(TestModels.Deal.class).relatedTo(new Object());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithNonBasicMemberTypes() throws Exception {
    mTestSubject.iNeed(TestModels.ClassWithNonBasicFieldType.class).in(mContentResolver);
  }

  @Test
  public void shouldNotGenerateFakeValuesForFieldsWithTreatNullAsDefaultMarker() throws Exception {
    TestModels.User user = mTestSubject.iNeed(TestModels.User.class).in(mContentResolver);

    assertThat(user).isNotNull();

    assertThat(user.updated_at).isEqualTo("now");
  }

  @Test
  public void shouldCreateKotlinClassObject() throws Exception {
    KotlinDataClass o = mTestSubject
        .iNeed(KotlinDataClass.class)
        .with("data", 21L)
        .in(mContentResolver);
    assertThat(o.getData()).isEqualTo(21L);
  }
}
