import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import AppPhase5Shell from "./AppPhase5Shell";
import { PepewLogoBadge } from "./components/PepewLogoBadge";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <PepewLogoBadge />
    <AppPhase5Shell />
  </StrictMode>,
);
