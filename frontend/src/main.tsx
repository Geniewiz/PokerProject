import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles.css";

(globalThis as { global?: typeof globalThis }).global = globalThis;

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
