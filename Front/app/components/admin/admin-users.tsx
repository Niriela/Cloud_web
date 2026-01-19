import { useEffect, useState } from "react";
import { Button } from "~/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "~/components/ui/card";
import { getUsers, resetUserBlock, type UserAdmin } from "~/lib/api";

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
                      <Button
                        size="sm"
                        type="button"
                        disabled={actionId === user.id}
                        onClick={() => handleReset(user.id)}
                      >
                        {actionId === user.id ? "En cours..." : "Debloquer"}
                      </Button>
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
