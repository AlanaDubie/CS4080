#include <stdio.h>
#include "common.h"
#include "chunk.h"
#include "debug.h"
#include "vm.h"

int main(void) {
  initVM();

  Chunk chunk;
  initChunk(&chunk);

  int constant = addConstant(&chunk, 1.2);
  writeChunk(&chunk, OP_CONSTANT, 123);
  writeChunk(&chunk, constant, 123);
  writeChunk(&chunk, OP_RETURN, 124);
  writeChunk(&chunk, OP_RETURN, 124);
  writeChunk(&chunk, OP_RETURN, 125);

  disassembleChunk(&chunk, "test chunk");

  // Check the runs directly
  for (int i = 0; i < chunk.lineCount; i++) {
    printf("run %d: line %d x%d\n", i,
           chunk.lines[i].line, chunk.lines[i].count);
  }

  // Check getLine at every offset (expected: 123 123 124 124 125)
  for (int i = 0; i < chunk.count; i++) {
    printf("getLine(%d) = %d\n", i, getLine(&chunk, i));
  }

  freeChunk(&chunk);
  freeVM();
  return 0;
}