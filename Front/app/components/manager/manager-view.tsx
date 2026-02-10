import { useEffect, useMemo, useState } from "react";
import {
  getEntreprises,
  getSignalements,
  getSignalementsStats,
  getStatuts,
  getTypeSignalements,
  deleteSignalement,
  updateSignalement,
  type Entreprise,
  type SignalementMapDto,
  type SignalementsStats,
  type Statut,
  type TypeSignalement,
} from "~/lib/api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "~/components/ui/card";
import { Button } from "~/components/ui/button";

type Draft = {
  surface: string;
  budget: string;
  statutsId: string;
  entrepriseId: string;
  typeSignalementId: string;
};

const buildDrafts = (items: SignalementMapDto[]) =>
  items.reduce<Record<number, Draft>>((acc, item) => {
    acc[item.id] = {
      surface: item.surface?.toString() ?? "",
      budget: item.budget?.toString() ?? "",
      statutsId: item.statutsId?.toString() ?? "",
      entrepriseId: item.entrepriseId?.toString() ?? "",
      typeSignalementId: item.typeSignalementId?.toString() ?? "",
    };
    return acc;
  }, {});

const numberOrNull = (value: string) =>
  value.trim() === "" ? null : Number(value);

const normalizeLabel = (value?: string | null) =>
  (value ?? "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

const normalizePercent = (value?: number | null) => {
  if (typeof value !== "number" || Number.isNaN(value)) {
    return 0;
  }
  return Math.max(0, Math.min(100, value));
};

const withLookups = (
  items: SignalementMapDto[],
  statuts: Statut[],
  entreprises: Entreprise[],
  types: TypeSignalement[],
) =>
  items.map((item) => ({
    ...item,
    statutsId:
      item.statut && statuts.length
        ? statuts.find(
            (s) => normalizeLabel(s.libelle) === normalizeLabel(item.statut),
          )?.id ?? null
        : item.statutsId,
    typeSignalementId:
      item.typeSignalement && types.length
        ? types.find(
            (t) =>
              normalizeLabel(t.libelle) === normalizeLabel(item.typeSignalement),
          )?.id ?? null
        : item.typeSignalementId,
    entrepriseId:
      item.entreprise && entreprises.length
        ? entreprises.find(
            (e) =>
              normalizeLabel(e.name) === normalizeLabel(item.entreprise),
          )?.id ?? null
        : item.entrepriseId,
  }));

export default function ManagerView() {
  const [signalements, setSignalements] = useState<SignalementMapDto[]>([]);
  const [stats, setStats] = useState<SignalementsStats | null>(null);
  const [statuts, setStatuts] = useState<Statut[]>([]);
  const [entreprises, setEntreprises] = useState<Entreprise[]>([]);
  const [types, setTypes] = useState<TypeSignalement[]>([]);
  const [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    Promise.all([
      getSignalements(),
      getSignalementsStats(),
      getStatuts(),
      getEntreprises(),
      getTypeSignalements(),
    ])
      .then(([signalementsData, statsData, statutsData, entreprisesData, typesData]) => {
        if (!active) return;
        const normalized = withLookups(
          signalementsData,
          statutsData,
          entreprisesData,
          typesData,
        );
        setSignalements(normalized);
        setStats(statsData);
        setStatuts(statutsData);
        setEntreprises(entreprisesData);
        setTypes(typesData);
        setDrafts(buildDrafts(normalized));
      })
      .catch(() => {
        if (!active) return;
        setError("Impossible de charger les données manager.");
      })
      .finally(() => {
        if (!active) return;
        setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const refreshStats = async () => {
    try {
      const statsData = await getSignalementsStats();
      setStats(statsData);
    } catch {
      // Ignore stats refresh error to keep CRUD usable.
    }
  };

  const handleDraftChange = (
    id: number,
    key: keyof Draft,
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
      const updated = await updateSignalement(id, {
        surface: numberOrNull(draft.surface),
        budget: numberOrNull(draft.budget),
        statutsId: numberOrNull(draft.statutsId),
        entrepriseId: numberOrNull(draft.entrepriseId),
        typeSignalementId: numberOrNull(draft.typeSignalementId),
      });
      setSignalements((prev) =>
        prev.map((item) => (item.id === id ? updated : item)),
      );
      setDrafts((prev) => ({
        ...prev,
        [id]: buildDrafts([updated])[updated.id],
      }));
      await refreshStats();
    } catch {
      setError("Impossible de sauvegarder le signalement.");
    } finally {
      setSavingId(null);
    }
  };

  const handleDelete = async (id: number) => {
    setDeletingId(id);
    setError(null);
    try {
      await deleteSignalement(id);
      setSignalements((prev) => prev.filter((item) => item.id !== id));
      setDrafts((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
      await refreshStats();
    } catch {
      setError("Impossible de supprimer le signalement.");
    } finally {
      setDeletingId(null);
    }
  };

  const rows = useMemo(() => signalements, [signalements]);
  const advancementPercent = normalizePercent(stats?.advancementPercent ?? 0);

  return (
    <Card>
      <CardHeader className="border-b">
        <CardTitle>Manager</CardTitle>
        <CardDescription>
          Gerez les informations des signalements et les statuts.
        </CardDescription>
        <div className="mt-3 rounded-md border border-gray-200 bg-gray-50 p-3">
          <div className="mb-1 flex items-center justify-between text-xs text-gray-700">
            <span>Avancement global</span>
            <span className="font-semibold">{advancementPercent.toFixed(1)}%</span>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
            <div
              className="h-full rounded-full bg-emerald-500 transition-all duration-300"
              style={{ width: `${advancementPercent}%` }}
            />
          </div>
        </div>
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
                  <th className="px-3 py-2 text-left">Type</th>
                  <th className="px-3 py-2 text-left">Description</th>
                  <th className="px-3 py-2 text-left">Statut</th>
                  <th className="px-3 py-2 text-left">Surface (m²)</th>
                  <th className="px-3 py-2 text-left">Budget</th>
                  <th className="px-3 py-2 text-left">Entreprise</th>
                  <th className="px-3 py-2 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((item) => {
                  const draft = drafts[item.id];
                  return (
                    <tr key={item.id} className="border-b last:border-0">
                      <td className="px-3 py-2">{item.id}</td>
                      <td className="px-3 py-2">
                        <select
                          className="w-full rounded-md border border-gray-200 bg-white px-2 py-1 text-xs"
                          value={draft?.typeSignalementId ?? ""}
                          onChange={(event) =>
                            handleDraftChange(
                              item.id,
                              "typeSignalementId",
                              event.target.value,
                            )
                          }
                        >
                          <option value="">-</option>
                          {types.map((type) => (
                            <option key={type.id} value={type.id}>
                              {type.libelle}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="px-3 py-2">
                        <span
                          className="block max-w-[240px] truncate"
                          title={item.description ?? undefined}
                        >
                          {item.description ?? "-"}
                        </span>
                      </td>
                      <td className="px-3 py-2">
                        <select
                          className="w-full rounded-md border border-gray-200 bg-white px-2 py-1 text-xs"
                          value={draft?.statutsId ?? ""}
                          onChange={(event) =>
                            handleDraftChange(
                              item.id,
                              "statutsId",
                              event.target.value,
                            )
                          }
                        >
                          <option value="">-</option>
                          {statuts.map((statut) => (
                            <option key={statut.id} value={statut.id}>
                              {statut.libelle}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="px-3 py-2">
                        <input
                          className="w-full rounded-md border border-gray-200 px-2 py-1 text-xs"
                          type="number"
                          step="0.01"
                          placeholder={item.surface?.toString() ?? ""}
                          value={draft?.surface ?? ""}
                          onChange={(event) =>
                            handleDraftChange(
                              item.id,
                              "surface",
                              event.target.value,
                            )
                          }
                        />
                      </td>
                      <td className="px-3 py-2">
                        <input
                          className="w-full rounded-md border border-gray-200 px-2 py-1 text-xs"
                          type="number"
                          step="0.01"
                          placeholder={item.budget?.toString() ?? ""}
                          value={draft?.budget ?? ""}
                          onChange={(event) =>
                            handleDraftChange(
                              item.id,
                              "budget",
                              event.target.value,
                            )
                          }
                        />
                      </td>
                      <td className="px-3 py-2">
                        <select
                          className="w-full rounded-md border border-gray-200 bg-white px-2 py-1 text-xs"
                          value={draft?.entrepriseId ?? ""}
                          onChange={(event) =>
                            handleDraftChange(
                              item.id,
                              "entrepriseId",
                              event.target.value,
                            )
                          }
                        >
                          <option value="">-</option>
                          {entreprises.map((entreprise) => (
                            <option key={entreprise.id} value={entreprise.id}>
                              {entreprise.name}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="px-3 py-2 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            size="sm"
                            type="button"
                            disabled={savingId === item.id || deletingId === item.id}
                            onClick={() => handleSave(item.id)}
                          >
                            {savingId === item.id ? "Sauvegarde..." : "Sauvegarder"}
                          </Button>
                          <Button
                            size="sm"
                            type="button"
                            variant="destructive"
                            disabled={savingId === item.id || deletingId === item.id}
                            onClick={() => handleDelete(item.id)}
                          >
                            {deletingId === item.id ? "Suppression..." : "Supprimer"}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {rows.length === 0 ? (
                  <tr>
                    <td
                      className="px-3 py-6 text-center text-sm text-muted-foreground"
                      colSpan={8}
                    >
                      Aucun signalement disponible.
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
