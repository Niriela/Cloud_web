import { useEffect, useMemo, useState } from "react";
import DelaisTable from "./delais-table";
import { fetchDelaisTraitement, type DelaiTraitement } from "~/lib/api";

type Tab = "overview" | "table";

const normalizeStatus = (value: string) =>
  value
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

export default function DelaisDashboard() {
  const [activeTab, setActiveTab] = useState<Tab>("overview");
  const [data, setData] = useState<DelaiTraitement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    fetchDelaisTraitement()
      .then((items) => {
        if (!active) return;
        setData(items);
      })
      .catch(() => {
        if (!active) return;
        setError("Impossible de charger les statistiques de delai.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const stats = useMemo(() => {
    const total = data.length;
    const totalDelai = data.reduce((sum, item) => sum + item.delaiJours, 0);
    const moyenne = total > 0 ? totalDelai / total : 0;
    const max = total > 0 ? Math.max(...data.map((item) => item.delaiJours)) : 0;
    const termines = data.filter(
      (item) => normalizeStatus(item.statutActuel) === "termine",
    ).length;
    const enCours = data.filter(
      (item) => normalizeStatus(item.statutActuel) === "en cours",
    ).length;
    const nouveaux = data.filter(
      (item) => normalizeStatus(item.statutActuel) === "nouveau",
    ).length;

    const distribution = new Map<string, number>();
    for (const item of data) {
      distribution.set(
        item.statutActuel,
        (distribution.get(item.statutActuel) ?? 0) + 1,
      );
    }

    const topDelais = [...data]
      .sort((a, b) => b.delaiJours - a.delaiJours)
      .slice(0, 5);

    return {
      total,
      moyenne,
      max,
      termines,
      enCours,
      nouveaux,
      distribution: Array.from(distribution.entries()),
      topDelais,
    };
  }, [data]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Dashboard des delais</h1>
          <p className="text-gray-600">Statistiques globales et indicateurs de performance</p>
        </div>
      </div>

      <div className="border-b">
        <nav className="flex space-x-8">
          <button
            onClick={() => setActiveTab("overview")}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === "overview"
                ? "border-blue-500 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
            }`}
          >
            Vue d'ensemble
          </button>
          <button
            onClick={() => setActiveTab("table")}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === "table"
                ? "border-blue-500 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
            }`}
          >
            Tableau des delais
          </button>
        </nav>
      </div>

      <div className="mt-6">
        {activeTab === "overview" && (
          <div className="space-y-6">
            {loading ? <div>Chargement...</div> : null}
            {error ? <p className="text-sm text-red-600">{error}</p> : null}

            {!loading && !error ? (
              <>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
                  <div className="rounded-lg border bg-white p-4">
                    <p className="text-xs text-gray-500">Signalements suivis</p>
                    <p className="mt-2 text-2xl font-semibold text-gray-900">{stats.total}</p>
                  </div>
                  <div className="rounded-lg border bg-white p-4">
                    <p className="text-xs text-gray-500">Delai moyen</p>
                    <p className="mt-2 text-2xl font-semibold text-gray-900">{stats.moyenne.toFixed(1)} jours</p>
                  </div>
                  <div className="rounded-lg border bg-white p-4">
                    <p className="text-xs text-gray-500">Delai max</p>
                    <p className="mt-2 text-2xl font-semibold text-gray-900">{stats.max} jours</p>
                  </div>
                  <div className="rounded-lg border bg-white p-4">
                    <p className="text-xs text-gray-500">Termines</p>
                    <p className="mt-2 text-2xl font-semibold text-gray-900">{stats.termines}</p>
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                  <div className="rounded-lg border bg-white p-4">
                    <h3 className="mb-4 text-sm font-semibold text-gray-900">Repartition par statut</h3>
                    <div className="space-y-3">
                      {stats.distribution.map(([label, count]) => {
                        const pct = stats.total > 0 ? (count / stats.total) * 100 : 0;
                        return (
                          <div key={label}>
                            <div className="mb-1 flex items-center justify-between text-xs text-gray-600">
                              <span>{label}</span>
                              <span>{count} ({pct.toFixed(1)}%)</span>
                            </div>
                            <div className="h-2 w-full rounded-full bg-gray-100">
                              <div
                                className="h-2 rounded-full bg-blue-500"
                                style={{ width: `${pct}%` }}
                              />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                    <div className="mt-4 grid grid-cols-3 gap-2 text-xs text-gray-600">
                      <span>Nouveau: {stats.nouveaux}</span>
                      <span>En cours: {stats.enCours}</span>
                      <span>Termine: {stats.termines}</span>
                    </div>
                  </div>

                  <div className="rounded-lg border bg-white p-4">
                    <h3 className="mb-4 text-sm font-semibold text-gray-900">Top 5 delais</h3>
                    <div className="space-y-3">
                      {stats.topDelais.map((item) => (
                        <div
                          key={item.signalementId}
                          className="flex items-center justify-between rounded-md border px-3 py-2"
                        >
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-gray-900">
                              #{item.signalementId} - {item.typeSignalement}
                            </p>
                            <p className="truncate text-xs text-gray-500">{item.description ?? "-"}</p>
                          </div>
                          <span className="ml-4 text-sm font-semibold text-red-600">{item.delaiJours} j</span>
                        </div>
                      ))}
                      {stats.topDelais.length === 0 ? (
                        <p className="text-sm text-gray-500">Aucune donnee disponible.</p>
                      ) : null}
                    </div>
                  </div>
                </div>
              </>
            ) : null}
          </div>
        )}

        {activeTab === "table" && <DelaisTable />}
      </div>
    </div>
  );
}
