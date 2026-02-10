// Front/app/components/manager/types-reparations.tsx
import { useEffect, useState } from "react";
import {
  getTypeSignalementsDto,
  createTypeSignalement,
  updateTypeSignalement,
  deleteTypeSignalement,
  calculateBudget,
  type TypeSignalement,
  type BudgetCalculation,
} from "~/lib/api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "~/components/ui/card";
import { Button } from "~/components/ui/button";
import { Input } from "~/components/ui/input";
import { Label } from "~/components/ui/label";
import {
  Plus,
  Pencil,
  Trash2,
  Calculator,
  Save,
  X,
  Wrench,
  AlertCircle,
  CheckCircle,
} from "lucide-react";

type EditingType = {
  id: number | null;
  libelle: string;
  niveau: number;
  prixParM2: number;
};

const initialEditing: EditingType = {
  id: null,
  libelle: "",
  niveau: 1,
  prixParM2: 0,
};

export default function TypesReparations() {
  const [types, setTypes] = useState<TypeSignalement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // État pour l'édition/création
  const [editing, setEditing] = useState<EditingType>(initialEditing);
  const [isCreating, setIsCreating] = useState(false);

  // État pour le calculateur de budget
  const [calculatorTypeId, setCalculatorTypeId] = useState<number | null>(null);
  const [calculatorSurface, setCalculatorSurface] = useState<string>("");
  const [budgetResult, setBudgetResult] = useState<BudgetCalculation | null>(null);

  useEffect(() => {
    loadTypes();
  }, []);

  useEffect(() => {
    if (success || error) {
      const timer = setTimeout(() => {
        setSuccess(null);
        setError(null);
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [success, error]);

  const loadTypes = async () => {
    try {
      setLoading(true);
      const data = await getTypeSignalementsDto();
      setTypes(data);
    } catch (err) {
      setError("Erreur lors du chargement des types de réparations");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setIsCreating(true);
    setEditing(initialEditing);
  };

  const handleEdit = (type: TypeSignalement) => {
    setIsCreating(false);
    setEditing({
      id: type.id,
      libelle: type.libelle,
      niveau: type.niveau,
      prixParM2: type.prixParM2,
    });
  };

  const handleCancel = () => {
    setIsCreating(false);
    setEditing(initialEditing);
    setBudgetResult(null);
    setCalculatorTypeId(null);
  };

  const handleSave = async () => {
    try {
      if (isCreating) {
        await createTypeSignalement({
          libelle: editing.libelle,
          niveau: editing.niveau,
          prixParM2: editing.prixParM2,
        });
        setSuccess("Type de réparation créé avec succès !");
      } else if (editing.id) {
        await updateTypeSignalement(editing.id, {
          libelle: editing.libelle,
          niveau: editing.niveau,
          prixParM2: editing.prixParM2,
        });
        setSuccess("Type de réparation mis à jour avec succès !");
      }
      handleCancel();
      loadTypes();
    } catch (err) {
      setError("Erreur lors de l'enregistrement");
      console.error(err);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Êtes-vous sûr de vouloir supprimer ce type de réparation ?")) {
      return;
    }
    try {
      await deleteTypeSignalement(id);
      setSuccess("Type de réparation supprimé avec succès !");
      loadTypes();
    } catch (err) {
      setError("Erreur lors de la suppression");
      console.error(err);
    }
  };

  const handleCalculateBudget = async () => {
    if (!calculatorTypeId || !calculatorSurface) return;
    try {
      const result = await calculateBudget(
        calculatorTypeId,
        parseFloat(calculatorSurface)
      );
      setBudgetResult(result);
    } catch (err) {
      setError("Erreur lors du calcul du budget");
      console.error(err);
    }
  };

  const getNiveauColor = (niveau: number) => {
    if (niveau <= 3) return "bg-emerald-100 text-emerald-800 border-emerald-300";
    if (niveau <= 6) return "bg-amber-100 text-amber-800 border-amber-300";
    return "bg-rose-100 text-rose-800 border-rose-300";
  };

  const getNiveauLabel = (niveau: number) => {
    if (niveau <= 3) return "Faible";
    if (niveau <= 6) return "Moyen";
    return "Élevé";
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        <span className="ml-3 text-gray-600">Chargement...</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Wrench className="h-6 w-6 text-blue-600" />
            Types de Réparations
          </h2>
          <p className="text-gray-500 mt-1">
            Gérez les catégories de réparations avec leurs niveaux et tarifs
          </p>
        </div>
        <Button
          onClick={handleCreate}
          className="bg-blue-600 hover:bg-blue-700 text-white"
        >
          <Plus className="h-4 w-4 mr-2" />
          Nouveau type
        </Button>
      </div>

      {/* Notifications */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-3">
          <AlertCircle className="h-5 w-5 text-red-500" />
          <span className="text-red-700">{error}</span>
        </div>
      )}
      {success && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 flex items-center gap-3">
          <CheckCircle className="h-5 w-5 text-green-500" />
          <span className="text-green-700">{success}</span>
        </div>
      )}

      {/* Formulaire création/édition */}
      {(isCreating || editing.id !== null) && (
        <Card className="border-blue-200 bg-blue-50/50">
          <CardHeader>
            <CardTitle className="text-lg">
              {isCreating ? "Nouveau type de réparation" : "Modifier le type"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="md:col-span-2">
                <Label htmlFor="libelle">Libellé</Label>
                <Input
                  id="libelle"
                  value={editing.libelle}
                  onChange={(e) =>
                    setEditing({ ...editing, libelle: e.target.value })
                  }
                  placeholder="Ex: Réparation toiture"
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="niveau">Niveau (1-10)</Label>
                <div className="flex items-center gap-2 mt-1">
                  <Input
                    id="niveau"
                    type="number"
                    min={1}
                    max={10}
                    value={editing.niveau}
                    onChange={(e) =>
                      setEditing({
                        ...editing,
                        niveau: Math.min(10, Math.max(1, parseInt(e.target.value) || 1)),
                      })
                    }
                    className="w-20"
                  />
                  <input
                    type="range"
                    min={1}
                    max={10}
                    value={editing.niveau}
                    onChange={(e) =>
                      setEditing({ ...editing, niveau: parseInt(e.target.value) })
                    }
                    className="flex-1"
                  />
                </div>
              </div>
              <div>
                <Label htmlFor="prixParM2">Prix par m² (Ar)</Label>
                <Input
                  id="prixParM2"
                  type="number"
                  min={0}
                  step={0.01}
                  value={editing.prixParM2}
                  onChange={(e) =>
                    setEditing({
                      ...editing,
                      prixParM2: parseFloat(e.target.value) || 0,
                    })
                  }
                  placeholder="0.00"
                  className="mt-1"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <Button variant="outline" onClick={handleCancel}>
                <X className="h-4 w-4 mr-2" />
                Annuler
              </Button>
              <Button
                onClick={handleSave}
                disabled={!editing.libelle.trim()}
                className="bg-green-600 hover:bg-green-700 text-white"
              >
                <Save className="h-4 w-4 mr-2" />
                Enregistrer
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Calculateur de budget */}
      <Card className="border-purple-200 bg-gradient-to-r from-purple-50 to-indigo-50">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-purple-800">
            <Calculator className="h-5 w-5" />
            Calculateur de Budget
          </CardTitle>
          <CardDescription>
            Formule: <code className="bg-white px-2 py-1 rounded text-purple-700">prix_par_m² × niveau × surface_m²</code>
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
            <div>
              <Label>Type de réparation</Label>
              <select
                value={calculatorTypeId ?? ""}
                onChange={(e) => {
                  setCalculatorTypeId(e.target.value ? parseInt(e.target.value) : null);
                  setBudgetResult(null);
                }}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 bg-white"
              >
                <option value="">Sélectionner...</option>
                {types.map((type) => (
                  <option key={type.id} value={type.id}>
                    {type.libelle} (Niv. {type.niveau})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <Label>Surface (m²)</Label>
              <Input
                type="number"
                min={0}
                step={0.1}
                value={calculatorSurface}
                onChange={(e) => {
                  setCalculatorSurface(e.target.value);
                  setBudgetResult(null);
                }}
                placeholder="Ex: 50"
                className="mt-1"
              />
            </div>
            <div>
              <Button
                onClick={handleCalculateBudget}
                disabled={!calculatorTypeId || !calculatorSurface}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white"
              >
                <Calculator className="h-4 w-4 mr-2" />
                Calculer
              </Button>
            </div>
            <div>
              {budgetResult && (
                <div className="bg-white rounded-lg p-4 border-2 border-purple-300 shadow-sm">
                  <div className="text-sm text-gray-500 mb-1">Budget estimé</div>
                  <div className="text-2xl font-bold text-purple-700">
                    {budgetResult.budgetEstime.toLocaleString("fr-FR", {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}{" "}
                    Ar
                  </div>
                  <div className="text-xs text-gray-400 mt-1">
                    {budgetResult.formule}
                  </div>
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Liste des types */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {types.map((type) => (
          <Card
            key={type.id}
            className="hover:shadow-lg transition-shadow duration-200 border-l-4"
            style={{
              borderLeftColor:
                type.niveau <= 3
                  ? "#10b981"
                  : type.niveau <= 6
                  ? "#f59e0b"
                  : "#ef4444",
            }}
          >
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg font-semibold text-gray-800">
                  {type.libelle}
                </CardTitle>
                <span
                  className={`px-2 py-1 text-xs font-medium rounded-full border ${getNiveauColor(
                    type.niveau
                  )}`}
                >
                  Niv. {type.niveau} - {getNiveauLabel(type.niveau)}
                </span>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {/* Barre de niveau visuelle */}
                <div>
                  <div className="flex justify-between text-xs text-gray-500 mb-1">
                    <span>Niveau de complexité</span>
                    <span>{type.niveau}/10</span>
                  </div>
                  <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-300"
                      style={{
                        width: `${(type.niveau / 10) * 100}%`,
                        backgroundColor:
                          type.niveau <= 3
                            ? "#10b981"
                            : type.niveau <= 6
                            ? "#f59e0b"
                            : "#ef4444",
                      }}
                    />
                  </div>
                </div>

                {/* Prix par m² */}
                <div className="bg-gray-50 rounded-lg p-3">
                  <div className="text-sm text-gray-500">Prix forfaitaire</div>
                  <div className="text-xl font-bold text-gray-800">
                    {type.prixParM2.toLocaleString("fr-FR", {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}{" "}
                    <span className="text-sm font-normal text-gray-500">
                      Ar/m²
                    </span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex gap-2 pt-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleEdit(type)}
                    className="flex-1"
                  >
                    <Pencil className="h-4 w-4 mr-1" />
                    Modifier
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleDelete(type.id)}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {types.length === 0 && !loading && (
        <Card className="border-dashed border-2 border-gray-300">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Wrench className="h-12 w-12 text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-1">
              Aucun type de réparation
            </h3>
            <p className="text-gray-500 mb-4">
              Commencez par créer votre premier type de réparation
            </p>
            <Button onClick={handleCreate} className="bg-blue-600 hover:bg-blue-700">
              <Plus className="h-4 w-4 mr-2" />
              Créer un type
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
