//> Hash Tables table-c
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#include "memory.h"
#include "object.h"
#include "table.h"
#include "value.h"

//> max-load
#define TABLE_MAX_LOAD 0.75

//< max-load
void initTable(Table* table) {
  table->count = 0;
  table->capacity = 0;
  table->entries = NULL;
}
//> free-table
void freeTable(Table* table) {
  FREE_ARRAY(Entry, table->entries, table->capacity);
  initTable(table);
}
static uint32_t hashValue(Value value) {
  if (IS_BOOL(value)) return AS_BOOL(value) ? 3 : 5;
  if (IS_NIL(value)) return 7;
  if (IS_NUMBER(value)) {
    double n = AS_NUMBER(value);
    if (n == 0) return 0;  // so 0 and -0 hash the same
    uint64_t bits;
    memcpy(&bits, &n, sizeof(bits));
    return (uint32_t)(bits ^ (bits >> 32));
  }
  if (IS_STRING(value)) return AS_STRING(value)->hash;
  return (uint32_t)(uintptr_t)AS_OBJ(value);
}

//< free-table
//> find-entry
//> omit
// NOTE: The "Optimization" chapter has a manual copy of this function.
// If you change it here, make sure to update that copy.
//< omit
static Entry* findEntry(Entry* entries, int capacity, Value key) {
  uint32_t index = hashValue(key) % capacity;
  Entry* tombstone = NULL;

  for (;;) {
    Entry* entry = &entries[index];
    if (!entry->isUsed) {
      if (IS_NIL(entry->value)) {
        return tombstone != NULL ? tombstone : entry;  // empty
      } else {
        if (tombstone == NULL) tombstone = entry;      // tombstone
      }
    } else if (valuesEqual(entry->key, key)) {
      return entry;
    }

    index = (index + 1) % capacity;
  }
}

//< find-entry
//> table-get
bool tableGet(Table* table, Value key, Value* value) {
  if (table->count == 0) return false;

  Entry* entry = findEntry(table->entries, table->capacity, key);
  if (!entry->isUsed) return false;

  *value = entry->value;
  return true;
}
//< table-get
//> table-adjust-capacity
static void adjustCapacity(Table* table, int capacity) {
  Entry* entries = ALLOCATE(Entry, capacity);

  for (int i = 0; i < capacity; i++) {
    entries[i].key = NIL_VAL;
    entries[i].value = NIL_VAL;
    entries[i].isUsed = false;
  }

  table->count = 0;
  for (int i = 0; i < table->capacity; i++) {
    Entry* entry = &table->entries[i];
    if (!entry->isUsed) continue;

    Entry* dest = findEntry(entries, capacity, entry->key);
    dest->key = entry->key;
    dest->value = entry->value;
    dest->isUsed = true;
    table->count++;
  }

  FREE_ARRAY(Entry, table->entries, table->capacity);
  table->entries = entries;
  table->capacity = capacity;
}

//< table-adjust-capacity
//> table-set
bool tableSet(Table* table, Value key, Value value) {
  if (table->count + 1 > table->capacity * TABLE_MAX_LOAD) {
    int capacity = GROW_CAPACITY(table->capacity);
    adjustCapacity(table, capacity);
  }

  Entry* entry = findEntry(table->entries, table->capacity, key);
  bool isNewKey = !entry->isUsed;
  if (isNewKey && IS_NIL(entry->value)) table->count++;

  entry->key = key;
  entry->value = value;
  entry->isUsed = true;
  return isNewKey;
}


//< table-set
//> table-delete
bool tableDelete(Table* table, Value key) {
  if (table->count == 0) return false;

  Entry* entry = findEntry(table->entries, table->capacity, key);
  if (!entry->isUsed) return false;

  // Place a tombstone in the entry.
  entry->key = NIL_VAL;
  entry->isUsed = false;
  entry->value = BOOL_VAL(true);
  return true;
}

//< table-delete
//> table-add-all
void tableAddAll(Table* from, Table* to) {
  for (int i = 0; i < from->capacity; i++) {
    Entry* entry = &from->entries[i];
    if (entry->isUsed) {
      tableSet(to, entry->key, entry->value);
    }
  }
}

ObjString* tableFindString(Table* table, const char* chars,
                           int length, uint32_t hash) {
  if (table->count == 0) return NULL;

  uint32_t index = hash & (table->capacity - 1);
  for (;;) {
    Entry* entry = &table->entries[index];
    if (!entry->isUsed) {
      // Stop if we find an empty non-tombstone entry.
      if (IS_NIL(entry->value)) return NULL;
    } else if (IS_STRING(entry->key)) {
      ObjString* s = AS_STRING(entry->key);
      if (s->length == length && s->hash == hash &&
          memcmp(s->chars, chars, length) == 0) {
        // We found it.
        return s;
      }
    }

    index = (index + 1) & (table->capacity - 1);
  }
}

//< table-find-string
//> Garbage Collection table-remove-white
void tableRemoveWhite(Table* table) {
  for (int i = 0; i < table->capacity; i++) {
    Entry* entry = &table->entries[i];
    if (entry->isUsed && IS_OBJ(entry->key) &&
        !AS_OBJ(entry->key)->isMarked) {
      tableDelete(table, entry->key);
    }
  }
}

//< Garbage Collection table-remove-white
//> Garbage Collection mark-table
void markTable(Table* table) {
  for (int i = 0; i < table->capacity; i++) {
    Entry* entry = &table->entries[i];
    markValue(entry->key);
    markValue(entry->value);
  }
}
//< Garbage Collection mark-table
