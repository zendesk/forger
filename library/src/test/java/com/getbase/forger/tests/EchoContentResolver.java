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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class EchoContentResolver {
  public static ContentResolver get() {
    final ContentResolver resolverMock = mock(ContentResolver.class);

    final Map<Uri, ContentValues> storedData = Maps.newHashMap();

    when(resolverMock.insert(any(Uri.class), any(ContentValues.class))).thenAnswer(new Answer<Uri>() {
      @Override
      public Uri answer(InvocationOnMock invocation) throws Throwable {
        final Object[] args = invocation.getArguments();
        Uri uri = (Uri) args[0];
        ContentValues values = (ContentValues) args[1];
        for (String key : getKeysOf(values)) {
          Object value = values.get(key);
          if (value instanceof Boolean) {
            values.put(key, ((Boolean) value) ? 1 : 0);
          }
        }

        Uri result = generateUri(uri);
        values.put(BaseColumns._ID, sId);

        if (!values.containsKey("updated_at")) {
          values.put("updated_at", "now");
        }

        storedData.put(result, values);
        return result;
      }
    });

    when(resolverMock.query(any(Uri.class), any(String[].class), anyString(), any(String[].class), anyString())).thenAnswer(new Answer<Cursor>() {
      @Override
      public Cursor answer(InvocationOnMock invocation) throws Throwable {
        final Object[] args = invocation.getArguments();
        Uri uri = (Uri) args[0];
        String[] projection = (String[]) args[1];

        Preconditions.checkState(storedData.containsKey(uri));
        final ContentValues values = storedData.get(uri);

        Set<String> storedColumns = Sets.newHashSet(getKeysOf(values));
        Preconditions.checkState(storedColumns.containsAll(Sets.newHashSet(projection)));

        MatrixCursor cursor = new MatrixCursor(projection, 1);
        cursor.addRow(Lists.transform(Lists.newArrayList(projection), new Function<String, Object>() {
          @Override
          public Object apply(String key) {
            return values.get(key);
          }
        }).toArray());

        return cursor;
      }
    });

    return resolverMock;
  }

  private static Collection<String> getKeysOf(ContentValues values) {
    return Collections2.transform(values.valueSet(), new Function<Map.Entry<String, Object>, String>() {
      @Override
      public String apply(Map.Entry<String, Object> entry) {
        return entry.getKey();
      }
    });
  }

  private static long sId = 0;

  private static Uri generateUri(Uri uri) {
    return ContentUris.withAppendedId(uri, ++sId);
  }
}
