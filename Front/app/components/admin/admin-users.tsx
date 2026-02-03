import { useEffect, useState } from "react";
import { Button } from "~/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "~/components/ui/card";
import { createManagerUser, getUsers, resetUserBlock, updateUser, type UserAdmin } from "~/lib/api";

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
  const [savingId, setSavingId] = useState<number | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);
  const [createMessage, setCreateMessage] = useState<string | null>(null);
  const [newUser, setNewUser] = useState({
    email: "",
    password: "",
    firstName: "",
    lastName: "",
  });
  const [drafts, setDrafts] = useState<Record<number, { email: string; firstName: string; lastName: string }>>({});

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    getUsers()
      .then((data) => {
        if (!active) return;
        setUsers(data);
        setDrafts(
          data.reduce<Record<number, { email: string; firstName: string; lastName: string }>>(
            (acc, user) => {
              acc[user.id] = {
                email: user.email ?? "",
                firstName: user.firstName ?? "",
                lastName: user.lastName ?? "",
              };
              return acc;
            },
            {},
          ),
        );
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

  const handleDraftChange = (
    id: number,
    key: "email" | "firstName" | "lastName",
    value: string,
  ) => {
    setDrafts((prev) => ({
      ...prev,
      [id]: {
        ...prev[id],
        [key]: value,
      },
    }));
  };

  const handleSave = async (id: number) => {
    const draft = drafts[id];
    if (!draft) return;
    setSavingId(id);
    setError(null);
    try {
      const updated = await updateUser(id, {
        email: draft.email,
        firstName: draft.firstName,
        lastName: draft.lastName,
      });
      setUsers((prev) => prev.map((user) => (user.id === id ? updated : user)));
      setDrafts((prev) => ({
        ...prev,
        [id]: {
          email: updated.email ?? "",
          firstName: updated.firstName ?? "",
          lastName: updated.lastName ?? "",
        },
      }));
    } catch {
      setError("Impossible de modifier l'utilisateur.");
    } finally {
      setSavingId(null);
    }
  };

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
                      <div className="flex flex-col gap-1">
                        <input
                          className="w-full rounded-md border border-gray-200 px-2 py-1 text-xs"
                          value={drafts[user.id]?.firstName ?? ""}
                          onChange={(event) =>
                            handleDraftChange(
                              user.id,
                              "firstName",
                              event.target.value,
                            )
                          }
                        />
                        <input
                          className="w-full rounded-md border border-gray-200 px-2 py-1 text-xs"
                          value={drafts[user.id]?.lastName ?? ""}
                          onChange={(event) =>
                            handleDraftChange(
                              user.id,
                              "lastName",
                              event.target.value,
                            )
                          }
                        />
                      </div>
                    </td>
                    <td className="px-3 py-2">
                      <input
                        className="w-full rounded-md border border-gray-200 px-2 py-1 text-xs"
                        value={drafts[user.id]?.email ?? ""}
                        onChange={(event) =>
                          handleDraftChange(user.id, "email", event.target.value)
                        }
                      />
                    </td>
                    <td className="px-3 py-2">{user.userType ?? "-"}</td>
                    <td className="px-3 py-2">{user.statutsUser ?? "-"}</td>
                    <td className="px-3 py-2">{user.failedLoginAttempts ?? 0}</td>
                    <td className="px-3 py-2">{formatDate(user.date)}</td>
                    <td className="px-3 py-2 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          size="sm"
                          type="button"
                          variant="outline"
                          disabled={savingId === user.id}
                          onClick={() => handleSave(user.id)}
                        >
                          {savingId === user.id ? "En cours..." : "Enregistrer"}
                        </Button>
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
                      </div>
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
