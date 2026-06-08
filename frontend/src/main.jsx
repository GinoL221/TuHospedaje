import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./App.css";
import "./layout.css";
import "react-datepicker/dist/react-datepicker.css";
import "./styles/datepicker.css";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <App />
  </StrictMode>
);
