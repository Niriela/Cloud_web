import { useEffect, useMemo, useState } from "react";
import {
  getEntreprises,
  getSignalements,
  getStatuts,
  getTypeSignalements,
  updateSignalement,
  type Entreprise,
  type SignalementMapDto,
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

export default function ManagerView() {
  const [signalements, setSignalements] = useState<SignalementMapDto[]>([]);
  const [statuts, setStatuts] = useState<Statut[]>([]);
  const [entreprises, setEntreprises] = useState<Entreprise[]>([]);
  const [types, setTypes] = useState<TypeSignalement[]>([]);
  const [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    Promise.all([
      getSignalements(),
      getStatuts(),
      getEntreprises(),
      getTypeSignalements(),
    ])
      .then(([signalementsData, statutsData, entreprisesData, typesData]) => {
        if (!active) return;
        setSignalements(signalementsData);
        setStatuts(statutsData);
        setEntreprises(entreprisesData);
        setTypes(typesData);
        setDrafts(buildDrafts(signalementsData));
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
    } catch {
      setError("Impossible de sauvegarder le signalement.");
    } finally {
      setSavingId(null);
    }
  };

  const rows = useMemo(() => signalements, [signalements]);

  return (
    <Card>
      <CardHeader className="border-b">
        <CardTitle>Manager</CardTitle>
        <CardDescription>
          Gerez les informations des signalements et les statuts.
        </CardDescription>
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
                  <th className="px-3 py-2 text-left">Statut</th>
                  <th className="px-3 py-2 text-left">Surface (m²)</th>
                  <th className="px-3 py-2 text-left">Budget</th>
                  <th className="px-3 py-2 text-left">Entreprise</th>
                  <th className="px-3 py-2 text-right">Action</th>
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
                        <Button
                          size="sm"
                          type="button"
                          disabled={savingId === item.id}
                          onClick={() => handleSave(item.id)}
                        >
                          {savingId === item.id ? "Sauvegarde..." : "Sauvegarder"}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
                {rows.length === 0 ? (
                  <tr>
                    <td
                      className="px-3 py-6 text-center text-sm text-muted-foreground"
                      colSpan={7}
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
