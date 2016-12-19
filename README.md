Forger
======
Android library for populating the ContentProvider with test data.

Features
========
The library is tightly coupled with MicroOrm and Thneed projects. You need to annotate your data models fields with MicroOrm's `@Column`:
```java
public class BaseModel {
  @Column("id")
  public long id;
}

public class Block extends BaseModel {
  @Column(Blocks.BLOCK_TITLE)
  public String title;

  @Column(Blocks.BLOCK_START)
  public long blockStart;

  @Column(Blocks.BLOCK_END)
  public long blockEnd;
}

public class Room extends BaseModel {
  @Column(Rooms.ROOM_NAME)
  public String name;

  @Column(Rooms.ROOM_FLOOR)
  public String floor;
}

public class Session extends BaseModel {
  @Column(Sessions.ROOM_ID)
  public long roomId;

  @Column(Sessions.BLOCK_ID)
  public long blockId;

  @Column(Sessions.SESSION_TITLE)
  public String title;
}

// more models definitions...
```

Then you have to define the relationships between your data models using Thneed:
```java
public interface DataModel extends ContentProviderModel, PojoModel {
}

private DataModel ROOM = new DataModel {
  // implementation
}

// ...

ModelGraph<DataModel> graph = ModelGraph.of(DataModel.class)
  .where()
  .the(SESSION).references(ROOM).by(Sessions.ROOM_ID)
  .the(SESSION).references(BLOCK).by(Sessions.BLOCK_ID)
  // more relationships definitions...
  .build();
```

All this allows you to write code like this in your tests:
```java
Forger<DataModel> forger = new Forger(graph, new MicroOrm());

// create a simple object
Room room = forger.iNeed(Room.class).in(myContentResolver);
Block block = forger.iNeed(Block.class).in(myContentResolver);

// create an object related to specific objects
Session session = forger.iNeed(Session.class)
  .relatedTo(room, block)
  .in(myContentResolver);

assertThat(session.room_id).isEqualTo(room.id);
assertThat(session.block_id).isEqualTo(block.id);

// create an object and automatically satisfy its dependencies
Session session = forger.iNeed(Session.class).in(myContentResolver);

assertThat(session.room_id).isNotEqualTo(0);
assertThat(session.block_id).isNotEqualTo(0);
```

It might look like a lot of boilerplate code, but it still beats writing all helper methods for your tests by hand, especially when the number of data models and connections between them grows to several dozens.

Usage
=====
Just add the dependency to your `build.gradle`:

```groovy
dependencies {
    testCompile 'com.getbase.android.forger:forger:0.2'
}
```

License
=======

    Copyright (C) 2013 Jerzy Chalupski

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License. 
