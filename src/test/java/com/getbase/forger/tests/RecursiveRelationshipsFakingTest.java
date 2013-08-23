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
public class RecursiveRelationshipsFakingTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<TestModels.TestModel>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test
  public void shouldAllowFakingOfObjectWithRecursiveRelationshipWithoutSuppliedObject() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);

    assertThat(contact).isNotNull();
    assertThat(contact.contactId).isNull();
  }

  @Test
  public void shouldAllowSupplyingObjectsForRecursiveRelationship() throws Exception {
    TestModels.Contact company = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(company).isNotNull();

    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).relatedTo(company).in(mContentResolver);
    assertThat(contact).isNotNull();
    assertThat(contact.contactId).isEqualTo(company.id);
  }

  @Test
  public void shouldSatisfyRecursiveDependenciesFromSuppliedContext() throws Exception {
    TestModels.Contact company = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(company).isNotNull();

    Forger<TestModels.TestModel> forgerWithContext = mTestSubject.inContextOf(company);

    TestModels.Contact contact = forgerWithContext.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();
    assertThat(contact.contactId).isEqualTo(company.id);
  }
}
