import { useEffect, useState } from "react";
import { Button } from "~/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "~/components/ui/card";
import { createManagerUser, getUsers, resetUserBlock, type UserAdmin } from "~/lib/api";

const formatDate = (value?: string | null) => {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString();
};

export default function AdminUsers() {
  const [users, setUsers] = useState<UserAdmin[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionId, setActionId] = useState<number | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);
  const [createMessage, setCreateMessage] = useState<string | null>(null);
  const [newUser, setNewUser] = useState({
    email: "",
    password: "",
    firstName: "",
    lastName: "",
  });

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    getUsers()
      .then((data) => {
        if (!active) return;
        setUsers(data);
      })
      .catch(() => {
        if (!active) return;
        setError("Impossible de charger les utilisateurs.");
      })
      .finally(() => {
        if (!active) return;
        setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const handleReset = async (userId: number) => {
    setActionId(userId);
    setError(null);
    try {
      await resetUserBlock(userId);
      const refreshed = await getUsers();
      setUsers(refreshed);
    } catch {
      setError("Impossible de debloquer l'utilisateur.");
    } finally {
      setActionId(null);
    }
  };

  const filteredUsers = users.filter(
    (user) => (user.userType ?? "").toLowerCase() === "utilisateur",
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle>Gestion des utilisateurs</CardTitle>
        <CardDescription>Debloquer les comptes et consulter les statuts.</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="mb-6 rounded-md border border-gray-200 bg-gray-50 p-4">
          <p className="mb-3 text-sm font-semibold text-gray-700">
            Creer un compte utilisateur
          </p>
          {createError ? (
            <p className="mb-2 text-xs text-red-600">{createError}</p>
          ) : null}
          {createMessage ? (
            <p className="mb-2 text-xs text-green-700">{createMessage}</p>
          ) : null}
          <div className="grid gap-3 md:grid-cols-2">
            <input
              className="w-full rounded-md border border-gray-200 px-3 py-2 text-xs"
              placeholder="Prenom"
              value={newUser.firstName}
              onChange={(event) =>
                setNewUser((prev) => ({ ...prev, firstName: event.target.value }))
              }
            />
            <input
              className="w-full rounded-md border border-gray-200 px-3 py-2 text-xs"
              placeholder="Nom"
              value={newUser.lastName}
              onChange={(event) =>
                setNewUser((prev) => ({ ...prev, lastName: event.target.value }))
              }
            />
            <input
              className="w-full rounded-md border border-gray-200 px-3 py-2 text-xs"
              placeholder="Email"
              type="email"
              value={newUser.email}
              onChange={(event) =>
                setNewUser((prev) => ({ ...prev, email: event.target.value }))
              }
            />
            <input
              className="w-full rounded-md border border-gray-200 px-3 py-2 text-xs"
              placeholder="Mot de passe"
              type="password"
              value={newUser.password}
              onChange={(event) =>
                setNewUser((prev) => ({ ...prev, password: event.target.value }))
              }
            />
          </div>
          <div className="mt-3 flex justify-end">
            <Button
              type="button"
              size="sm"
              onClick={async () => {
                setCreateError(null);
                setCreateMessage(null);
                try {
                  await createManagerUser(newUser);
                  setCreateMessage("Compte utilisateur cree.");
                  setNewUser({
                    email: "",
                    password: "",
                    firstName: "",
                    lastName: "",
                  });
                  const refreshed = await getUsers();
                  setUsers(refreshed);
                } catch {
                  setCreateError("Impossible de creer le compte.");
                }
              }}
            >
              Creer
            </Button>
          </div>
        </div>
        {error ? <p className="mb-3 text-xs text-red-600">{error}</p> : null}
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Chargement...</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-xs uppercase text-muted-foreground">
                <tr className="border-b">
                  <th className="px-3 py-2 text-left">ID</th>
                  <th className="px-3 py-2 text-left">Nom</th>
                  <th className="px-3 py-2 text-left">Email</th>
                  <th className="px-3 py-2 text-left">Type</th>
                  <th className="px-3 py-2 text-left">Statut</th>
                  <th className="px-3 py-2 text-left">Tentatives</th>
                  <th className="px-3 py-2 text-left">Cree le</th>
                  <th className="px-3 py-2 text-right">Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id} className="border-b last:border-0">
                    <td className="px-3 py-2">{user.id}</td>
                    <td className="px-3 py-2">
                      {user.firstName} {user.lastName}
                    </td>
                    <td className="px-3 py-2">{user.email}</td>
                    <td className="px-3 py-2">{user.userType ?? "-"}</td>
                    <td className="px-3 py-2">{user.statutsUser ?? "-"}</td>
                    <td className="px-3 py-2">{user.failedLoginAttempts ?? 0}</td>
                    <td className="px-3 py-2">{formatDate(user.date)}</td>
                    <td className="px-3 py-2 text-right">
                      {user.statutsUser?.toLowerCase() === "bloque" ? (
                        <Button
                          size="sm"
                          type="button"
                          disabled={actionId === user.id}
                          onClick={() => handleReset(user.id)}
                        >
                          {actionId === user.id ? "En cours..." : "Debloquer"}
                        </Button>
                      ) : (
                        <span className="text-xs text-muted-foreground">-</span>
                      )}
                    </td>
                  </tr>
                ))}
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td
                      className="px-3 py-6 text-center text-sm text-muted-foreground"
                      colSpan={8}
                    >
                      Aucun utilisateur.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
