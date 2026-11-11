#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "common.h"
#include "table.h"
#include "vm.h"

static double now() { return (double)clock() / CLOCKS_PER_SEC; }

static void sequentialInsert(int n) {
  Table t; initTable(&t);
  double s = now();
  for (int i = 0; i < n; i++) tableSet(&t, NUMBER_VAL(i), NUMBER_VAL(i));
  printf("sequential insert %d: %.3fs\n", n, now() - s);
  freeTable(&t);
}

static void randomInsert(int n) {
  Table t; initTable(&t);
  srand(1);
  double s = now();
  for (int i = 0; i < n; i++) tableSet(&t, NUMBER_VAL(rand()), NUMBER_VAL(i));
  printf("random insert %d: %.3fs\n", n, now() - s);
  freeTable(&t);
}

static void lookups(int n) {
  Table t; initTable(&t);
  for (int i = 0; i < n; i++) tableSet(&t, NUMBER_VAL(i), NUMBER_VAL(i));
  Value v;
  double s = now();
  for (int i = 0; i < n; i++) tableGet(&t, NUMBER_VAL(i), &v);
  printf("lookup hits %d: %.3fs\n", n, now() - s);
  s = now();
  for (int i = 0; i < n; i++) tableGet(&t, NUMBER_VAL(i + n), &v);
  printf("lookup misses %d: %.3fs\n", n, now() - s);
  freeTable(&t);
}

static void deleteChurn(int n) {
  Table t; initTable(&t);
  for (int i = 0; i < n; i++) tableSet(&t, NUMBER_VAL(i), NUMBER_VAL(i));
  double s = now();
  for (int i = 0; i < n * 5; i++) {
    tableDelete(&t, NUMBER_VAL(i));
    tableSet(&t, NUMBER_VAL(i + n), NUMBER_VAL(i));
  }
  printf("delete churn %d: %.3fs\n", n, now() - s);
  freeTable(&t);
}

int main() {
  initVM();
  int sizes[] = {1000, 10000, 100000};
  for (int i = 0; i < 3; i++) {
    sequentialInsert(sizes[i]);
    randomInsert(sizes[i]);
    lookups(sizes[i]);
    deleteChurn(sizes[i]);
    printf("\n");
  }
  freeVM();
  return 0;
}