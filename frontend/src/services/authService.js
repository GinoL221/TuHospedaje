import { get, post, bootstrapCsrf } from "./api";

export function getCurrentUser() {
  return get("/auth/me").then((data) => bootstrapCsrf().then(() => data));
}

export function login(email, password) {
  return post("/auth/login", { email, password }).then((data) =>
    bootstrapCsrf().then(() => data),
  );
}

export function register(firstName, lastName, email, password) {
  return post("/auth/register", { firstName, lastName, email, password }).then(
    (data) =>
      bootstrapCsrf()
        .then(() => data)
        .catch(() => {
          throw new Error(
            "Tu cuenta se creó correctamente, pero no pudimos iniciar sesión automáticamente. Iniciá sesión con tus credenciales.",
          );
        }),
  );
}

export function logout() {
  return post("/auth/logout");
}
