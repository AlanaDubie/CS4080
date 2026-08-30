#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct Node {
    char *data;
    struct Node *prev;
    struct Node *next;
} Node;

typedef struct {
    Node *head;
    Node *tail;
} List;

void list_init(List *list) {
    list->head = NULL;
    list->tail = NULL;
}

void list_insert(List *list, const char *value) {
    Node *node = malloc(sizeof(Node));
    if (node == NULL) {
        fprintf(stderr, "out of memory\n");
        exit(1);
    }

    node->data = malloc(strlen(value) + 1);
    if (node->data == NULL) {
        free(node);
        fprintf(stderr, "out of memory\n");
        exit(1);
    }
    strcpy(node->data, value);

    node->next = NULL;
    node->prev = list->tail;

    if (list->tail != NULL) {
        list->tail->next = node;
    } else {
        list->head = node;
    }
    list->tail = node;
}

Node *list_find(List *list, const char *value) {
    Node *current = list->head;
    while (current != NULL) {
        if (strcmp(current->data, value) == 0) {
            return current;
        }
        current = current->next;
    }
    return NULL;
}

int list_delete(List *list, const char *value) {
    Node *target = list_find(list, value);
    if (target == NULL) {
        return 0;
    }

    if (target->prev != NULL) {
        target->prev->next = target->next;
    } else {
        list->head = target->next;
    }

    if (target->next != NULL) {
        target->next->prev = target->prev;
    } else {
        list->tail = target->prev;
    }

    free(target->data);
    free(target);
    return 1;
}

void list_print(List *list) {
    Node *current = list->head;
    while (current != NULL) {
        printf("%s -> ", current->data);
        current = current->next;
    }
    printf("NULL\n");
}

void list_free(List *list) {
    Node *current = list->head;
    while (current != NULL) {
        Node *next = current->next;
        free(current->data);
        free(current);
        current = next;
    }
    list->head = NULL;
    list->tail = NULL;
}

int main(void) {
    List list;
    list_init(&list);

    list_insert(&list, "apple");
    list_insert(&list, "banana");
    list_print(&list);

    list_delete(&list, "apple");
    list_print(&list);

    list_free(&list);
    return 0;
}