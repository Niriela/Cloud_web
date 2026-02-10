// Front/app/routes.ts
import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("routes/home.tsx"),
  route("/login", "routes/login.tsx"),
  route("/logout", "routes/logout.tsx"),
  route("/dashboard", "routes/dashboard.tsx"),
  route("/manager", "routes/manager.tsx"),
  route("/admin", "routes/admin.tsx"),
  route("/Visiteurs", "routes/Visiteurs.tsx"),
  // Ajouter la route des statistiques
  route("/statistiques-delais", "routes/statistiques-delais.tsx"),
] satisfies RouteConfig;