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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.getbase.android.forger.Forger;

import org.chalup.microorm.MicroOrm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;
import android.content.ContentValues;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class OneToOneRelationshipsFakingTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<TestModels.TestModel>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test
  public void shouldSatisfyOneToOneDependenciesFromSuppliedContext() throws Exception {
    TestModels.Lead lead = mTestSubject.iNeed(TestModels.Lead.class).in(mContentResolver);
    assertThat(lead).isNotNull();

    Forger<TestModels.TestModel> forgerWithContext = mTestSubject.inContextOf(lead);

    TestModels.ContactData contactData = forgerWithContext.iNeed(TestModels.ContactData.class).in(mContentResolver);
    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(lead.id);
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInOneToOneRelationship() throws Exception {
    TestModels.ContactData contactData = mTestSubject.iNeed(TestModels.ContactData.class).in(mContentResolver);

    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingParentObjectForOneToOneRelationship() throws Exception {
    TestModels.Lead lead = mTestSubject.iNeed(TestModels.Lead.class).in(mContentResolver);
    assertThat(lead).isNotNull();

    TestModels.ContactData contactData = mTestSubject.iNeed(TestModels.ContactData.class).relatedTo(lead).in(mContentResolver);
    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(lead.id);
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfOneToOneRelationship() throws Exception {
    long leadId = 42L;
    TestModels.ContactData contactData = mTestSubject.iNeed(TestModels.ContactData.class).with("lead_id", leadId).in(mContentResolver);

    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(leadId);

    verify(mContentResolver, never()).insert(Matchers.eq(TestModels.LEAD.getUri()), any(ContentValues.class));
  }
}
