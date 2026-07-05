import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import AppPhase5Shell from "./AppPhase5Shell";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AppPhase5Shell />
  </StrictMode>,
);
