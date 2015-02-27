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
public class ManyToManyRelationshipsFakingTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<TestModels.TestModel>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfManyToManyRelationship() throws Exception {
    long contactId = 42L;
    long dealId = 7L;
    TestModels.DealContact dealContact = mTestSubject.iNeed(TestModels.DealContact.class)
        .with("contact_id", contactId)
        .with("deal_id", dealId)
        .in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contactId);
    assertThat(dealContact.dealId).isEqualTo(dealId);

    verify(mContentResolver, never()).insert(Matchers.eq(TestModels.CONTACT.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(Matchers.eq(TestModels.DEAL.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldCreateObjectForPartiallySatisfiedDependenciesOfManyToManyRelationship() throws Exception {
    long dealId = 42L;
    TestModels.DealContact dealContact = mTestSubject.iNeed(TestModels.DealContact.class)
        .with("deal_id", dealId)
        .in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isNotEqualTo(0);
    assertThat(dealContact.dealId).isEqualTo(dealId);

    verify(mContentResolver, never()).insert(Matchers.eq(TestModels.DEAL.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldCreateObjectWhenOneSideOfManyToManyRelationshipIsPolymorphic() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Tagging tagging = mTestSubject.iNeed(TestModels.Tagging.class).relatedTo(contact).in(mContentResolver);
    assertThat(tagging).isNotNull();
    assertThat(tagging.taggableType).isEqualTo("Contact");
    assertThat(tagging.taggableId).isEqualTo(contact.id);
    assertThat(tagging.userId).isNotEqualTo(0);
    assertThat(tagging.tagId).isNotEqualTo(0);
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInManyToManyRelationship() throws Exception {
    TestModels.DealContact dealContact = mTestSubject.iNeed(TestModels.DealContact.class).in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isNotEqualTo(0);
    assertThat(dealContact.dealId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingObjectsForManyToManyRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    TestModels.DealContact dealContact = mTestSubject
        .iNeed(TestModels.DealContact.class)
        .relatedTo(contact, deal)
        .in(mContentResolver);
    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contact.id);
    assertThat(dealContact.dealId).isEqualTo(deal.id);
  }

  @Test
  public void shouldAllowSupplyingObjectsForOnlyOneSideOfManyToManyRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.DealContact dealContact = mTestSubject
        .iNeed(TestModels.DealContact.class)
        .relatedTo(contact)
        .in(mContentResolver);
    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contact.id);
    assertThat(dealContact.dealId).isNotEqualTo(0);
  }
}
