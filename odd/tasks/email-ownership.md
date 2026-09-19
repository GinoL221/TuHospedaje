# Email Ownership

## Goal

Leave one productive email path: domain enqueue → template renderer → outbox persist → dispatcher → SMTP transport.

## Non-goals

- Changing visible HTML/subjects of current outbox emails
- Adding console transport
- Hexagonal rewrite

## Tasks

- [x] 1. Remove unused `EmailService` and keep SMTP as `EmailTransport` only.
- [x] 2. Move reservation confirmation/cancellation HTML into a renderer used by outbox enqueue.
