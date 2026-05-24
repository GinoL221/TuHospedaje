---
id: category-management
title: Gestión de Categorías
status: draft
date: 2026-05-21
version: "1.0"
---

# Category Management Specification

## Purpose

CRUD de categorías para alojamientos, asignación de categoría a alojamientos, visualización de categoría en respuestas de alojamiento, y consulta pública de categorías. Covers user stories #12, #20, #21 del Sprint 2.

## Requirements

### Requirement: Category CRUD

The system MUST allow administrators to create, list, update, and delete categories. Each category MUST have a unique name and an optional description.

#### Scenario: Create category with valid data

- GIVEN an authenticated admin user
- WHEN POST `/api/categories` with body `{ "name": "Hotel 5 estrellas", "description": "Alojamientos de lujo" }`
- THEN returns HTTP 201 with created category `{ "id": 1, "name": "Hotel 5 estrellas", "description": "Alojamientos de lujo" }`

#### Scenario: Create category with empty name

- GIVEN an authenticated admin user
- WHEN POST `/api/categories` with body `{ "name": "", "description": "Test" }`
- THEN returns HTTP 400 with validation error indicating name is required

#### Scenario: Create category with duplicate name

- GIVEN a category exists with name "Cabaña"
- WHEN POST `/api/categories` with body `{ "name": "Cabaña", "description": "Otra" }`
- THEN returns HTTP 409 with "category name already exists"

#### Scenario: List all categories

- GIVEN categories exist: "Hotel", "Cabaña", "Hostel"
- WHEN GET `/api/categories`
- THEN returns HTTP 200 with array of all categories sorted by name

#### Scenario: Update existing category

- GIVEN a category exists with id 1, name "Hotel"
- WHEN PUT `/api/categories/1` with body `{ "name": "Hotel boutique", "description": "Actualizado" }`
- THEN returns HTTP 200 with updated category `{ "id": 1, "name": "Hotel boutique", "description": "Actualizado" }`

#### Scenario: Update category with duplicate name

- GIVEN categories exist: id 1 "Hotel", id 2 "Cabaña"
- WHEN PUT `/api/categories/1` with body `{ "name": "Cabaña", "description": "Test" }`
- THEN returns HTTP 409 with "category name already exists"

#### Scenario: Delete existing category

- GIVEN a category exists with id 3, no lodgings associated
- WHEN DELETE `/api/categories/3`
- THEN returns HTTP 204 and category is no longer listed

#### Scenario: Delete non-existent category

- GIVEN no category exists with id 999
- WHEN DELETE `/api/categories/999`
- THEN returns HTTP 404

### Requirement: Assign Category to Lodging

The system MUST allow assigning a category to a lodging during creation or update. The category field MUST be nullable — a lodging MAY exist without a category.

#### Scenario: Create lodging with valid category

- GIVEN a category exists with id 1, name "Hotel"
- WHEN POST `/api/lodgings` with body `{ "name": "Gran Hotel", "categoryId": 1 }`
- THEN returns HTTP 201 with lodging including `categoryId: 1` and `categoryName: "Hotel"`

#### Scenario: Create lodging without category

- GIVEN no category is specified
- WHEN POST `/api/lodgings` with body `{ "name": "Alquiler temporal" }`
- THEN returns HTTP 201 with lodging including `categoryId: null` and `categoryName: null`

#### Scenario: Change category of existing lodging

- GIVEN a lodging exists with id 5, categoryId: 1 ("Hotel")
- WHEN PUT `/api/lodgings/5` with body `{ "name": "Gran Hotel", "categoryId": 2 }` and category 2 exists as "Cabaña"
- THEN returns HTTP 200 with lodging including `categoryId: 2` and `categoryName: "Cabaña"`

#### Scenario: Assign non-existent category

- GIVEN no category exists with id 999
- WHEN POST `/api/lodgings` with body `{ "name": "Test", "categoryId": 999 }`
- THEN returns HTTP 404 with "category not found"

### Requirement: Display Category in Lodging Response

The system MUST include category information when returning lodging data. Each lodging response MUST include `categoryId` and `categoryName` fields.

#### Scenario: GET single lodging includes category

- GIVEN a lodging exists with id 5, associated with category "Hotel" (id 1)
- WHEN GET `/api/lodgings/5`
- THEN returns HTTP 200 with body including `categoryId: 1` and `categoryName: "Hotel"`

#### Scenario: GET lodgings list includes category per item

- GIVEN lodgings exist: id 1 with category "Hotel", id 2 with category "Cabaña", id 3 with no category
- WHEN GET `/api/lodgings`
- THEN returns HTTP 200 with array where item 1 has `categoryId: 1, categoryName: "Hotel"`, item 2 has `categoryId: 2, categoryName: "Cabaña"`, item 3 has `categoryId: null, categoryName: null`

#### Scenario: Lodging without category returns null

- GIVEN a lodging exists with id 10, no category assigned
- WHEN GET `/api/lodgings/10`
- THEN returns HTTP 200 with body including `categoryId: null` and `categoryName: null`

### Requirement: Public Category Query

The system MUST allow unauthenticated users to query categories. GET endpoints for categories MUST NOT require a JWT token.

#### Scenario: List categories without authentication

- GIVEN no authentication token is provided
- WHEN GET `/api/categories`
- THEN returns HTTP 200 with array of all categories

#### Scenario: Get single category without authentication

- GIVEN a category exists with id 1
- WHEN GET `/api/categories/1` with no authentication token
- THEN returns HTTP 200 with category data

### Requirement: Write Security for Categories

The system MUST require a valid JWT token for creating, updating, or deleting categories. Unauthenticated write requests MUST be rejected.

#### Scenario: Create category without token

- GIVEN no authentication token
- WHEN POST `/api/categories` with body `{ "name": "Hotel", "description": "Test" }`
- THEN returns HTTP 401 or 403

#### Scenario: Update category without token

- GIVEN a category exists with id 1
- WHEN PUT `/api/categories/1` with body `{ "name": "Updated" }` and no token
- THEN returns HTTP 401 or 403

#### Scenario: Delete category without token

- GIVEN a category exists with id 1
- WHEN DELETE `/api/categories/1` with no token
- THEN returns HTTP 401 or 403

#### Scenario: Write operations with valid token

- GIVEN a valid JWT token with ADMIN role
- WHEN POST/PUT/DELETE `/api/categories` with `Bearer <token>`
- THEN request is processed based on user authorization
