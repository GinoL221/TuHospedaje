import { postMultipart } from "./api";

export function uploadImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  return postMultipart("/upload", formData);
}
