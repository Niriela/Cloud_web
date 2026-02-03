import { useEffect, useMemo, useRef, useState } from "react";
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
  getFirestoreEntreprises,
  getFirestoreStatuts,
  getFirestoreTypeSignalements,
  mapFirestoreSignalementsRaw,
  normalizeLabel,
  updateFirestoreSignalement,
} from "~/lib/firestore-data";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "~/components/ui/card";
import { Button } from "~/components/ui/button";
import {
  collection,
  getFirestore,
  onSnapshot,
  query,
  limit,
} from "firebase/firestore";
import { firebaseAuth } from "~/lib/firebase";

type Draft = {
  surface: string;
  budget: string;
  statutsId: string;
  entrepriseId: string;
  typeSignalementId: string;
};

const FIRESTORE_SIGNAL_LIMIT = 200;

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
            (e) => normalizeLabel(e.name) === normalizeLabel(item.entreprise),
          )?.id ?? null
        : item.entrepriseId,
  }));

export default function ManagerView() {
  const [signalements, setSignalements] = useState<SignalementMapDto[]>([]);
  const [statuts, setStatuts] = useState<Statut[]>([]);
  const [entreprises, setEntreprises] = useState<Entreprise[]>([]);
  const [types, setTypes] = useState<TypeSignalement[]>([]);
  const [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFirestoreSource, setIsFirestoreSource] = useState(false);

  // Pour éviter que onSnapshot se réabonne à cause des deps
  const lookupsRef = useRef({
    statuts: [] as Statut[],
    entreprises: [] as Entreprise[],
    types: [] as TypeSignalement[],
  });

  useEffect(() => {
    lookupsRef.current = { statuts, entreprises, types };
  }, [statuts, entreprises, types]);

  const fallbackToApi = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [signalementsData, statutsData, entreprisesData, typesData] =
        await Promise.all([
          getSignalements(),
          getStatuts(),
          getEntreprises(),
          getTypeSignalements(),
        ]);

      const normalized = withLookups(
        signalementsData,
        statutsData,
        entreprisesData,
        typesData,
      );

      setSignalements(normalized);
      setStatuts(statutsData);
      setEntreprises(entreprisesData);
      setTypes(typesData);
      setDrafts(buildDrafts(normalized));
      setIsFirestoreSource(false);
    } catch {
      setError("Impossible de charger les données manager.");
    } finally {
      setIsLoading(false);
    }
  };

  // Chargement initial
  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError(null);

    const load = async () => {
      const online =
        typeof navigator !== "undefined" ? navigator.onLine : true;

      // On essaie Firestore (mais uniquement pour les lookups, PAS les signalements)
      if (online) {
        try {
          const [statutsData, entreprisesData, typesData] = await Promise.all([
            getFirestoreStatuts(),
            getFirestoreEntreprises(),
            getFirestoreTypeSignalements(),
          ]);

          return {
            source: "firestore" as const,
            statutsData,
            entreprisesData,
            typesData,
          };
        } catch {
          // ignore -> fallback API
        }
      }

      return { source: "api" as const };
    };

    load()
      .then(async (result) => {
        if (!active) return;

        if (result.source === "firestore") {
          setStatuts(result.statutsData);
          setEntreprises(result.entreprisesData);
          setTypes(result.typesData);
          setIsFirestoreSource(true);

          // On laisse onSnapshot charger signalements (pas de double lecture)
          setSignalements([]);
          setDrafts({});
          return;
        }

        // API fallback
        await fallbackToApi();
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Listener Firestore (limité + sans re-subscribe)
  useEffect(() => {
    const online =
      typeof navigator !== "undefined" ? navigator.onLine : true;

    if (!isFirestoreSource || !online) return;

    const db = getFirestore(firebaseAuth.app);

    // IMPORTANT: limiter sinon tu lis toute la collection
    // Optionnel: si tu as un champ "updatedAt" ou "createdAt", tu peux ajouter orderBy.
    // Exemple:
    // const q = query(collection(db, "signalements"), orderBy("updatedAt", "desc"), limit(FIRESTORE_SIGNAL_LIMIT));
    const q = query(
      collection(db, "signalements"),
      limit(FIRESTORE_SIGNAL_LIMIT),
    );

    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        try {
          const { statuts, entreprises, types } = lookupsRef.current;

          // Si ton mapping a besoin de l'id Firestore, remplace par:
          // snapshot.docs.map((d) => ({ id: d.id, ...d.data() }))
          const raw = snapshot.docs.map((doc) => doc.data());

          const mapped = mapFirestoreSignalementsRaw(
            raw,
            statuts,
            entreprises,
            types,
          );
          const normalized = withLookups(mapped, statuts, entreprises, types);

          setSignalements(normalized);
          setDrafts(buildDrafts(normalized));
        } catch {
          setError("Impossible de charger les données manager.");
        }
      },
      async (err: any) => {
        // Quota dépassé -> on bascule sur API automatiquement
        if (err?.code === "resource-exhausted") {
          setError("Quota Firestore dépassé. Passage en mode API.");
          setIsFirestoreSource(false);
          await fallbackToApi();
          return;
        }
        setError("Impossible de charger les données manager.");
      },
    );

    return () => unsubscribe();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isFirestoreSource]);

  const handleDraftChange = (id: number, key: keyof Draft, value: string) => {
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
      const updated = isFirestoreSource
        ? await updateFirestoreSignalement(id, {
            surface: numberOrNull(draft.surface),
            budget: numberOrNull(draft.budget),
            statutsId: numberOrNull(draft.statutsId),
            typeSignalementId: numberOrNull(draft.typeSignalementId),
            entrepriseId: numberOrNull(draft.entrepriseId),
            statutsLabel:
              statuts.find((item) => item.id === numberOrNull(draft.statutsId))
                ?.libelle ?? null,
            typeLabel:
              types.find(
                (item) => item.id === numberOrNull(draft.typeSignalementId),
              )?.libelle ?? null,
            entrepriseLabel:
              entreprises.find(
                (item) => item.id === numberOrNull(draft.entrepriseId),
              )?.name ?? null,
          })
        : await updateSignalement(id, {
            surface: numberOrNull(draft.surface),
            budget: numberOrNull(draft.budget),
            statutsId: numberOrNull(draft.statutsId),
            entrepriseId: numberOrNull(draft.entrepriseId),
            typeSignalementId: numberOrNull(draft.typeSignalementId),
          });

      // Si Firestore est la source, le listener mettra à jour aussi,
      // mais on garde ceci pour un retour immédiat UI.
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
                  <th className="px-3 py-2 text-left">Description</th>
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
