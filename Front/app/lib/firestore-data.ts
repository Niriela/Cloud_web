import {
  Timestamp,
  collection,
  getDocs,
  getFirestore,
} from "firebase/firestore"
import { firebaseAuth } from "~/lib/firebase"
import type {
  SignalementMapDto,
  SignalementsStats,
  TypeSignalement,
  Statut,
  Entreprise,
} from "~/lib/api"
import { doc, getDoc, query, updateDoc, where } from "firebase/firestore"

type FirestoreSignalement = {
  id?: number | string
  title?: string | null
  status?: string | null
  description?: string | null
  latitude?: number | string | null
  longitude?: number | string | null
  surface?: number | string | null
  budget?: number | string | null
  entreprise?: string | null
  entreprise_id?: number | string
  statuts_id?: number | string
  type_signalement_id?: number | string
  createdAt?: Timestamp | string | null
  updatedAt?: Timestamp | string | null
}

type FirestoreStatut = {
  id?: number | string
  libelle?: string | null
}

type FirestoreEntreprise = {
  id?: number | string
  name?: string | null
}

type FirestoreTypeSignalement = {
  id?: number | string
  libelle?: string | null
}

export const toNumber = (value: unknown): number | null => {
  if (typeof value === "number" && !Number.isNaN(value)) {
    return value
  }
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value)
    return Number.isNaN(parsed) ? null : parsed
  }
  return null
}

export const normalizeLabel = (value?: string | null) =>
  (value ?? "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")

const isCompleted = (status?: string | null) => {
  const value = normalizeLabel(status)
  return value === "terminé" || value === "termine"
}

export const toDateString = (value?: Timestamp | string | null) => {
  if (!value) {
    return null
  }
  if (value instanceof Timestamp) {
    return value.toDate().toISOString()
  }
  if (typeof value === "string") {
    return value
  }
  return null
}

async function getCollection<T>(name: string): Promise<T[]> {
  const db = getFirestore(firebaseAuth.app)
  const snapshot = await getDocs(collection(db, name))
  return snapshot.docs.map((doc) => doc.data() as T)
}

export async function getFirestoreTypeSignalements(): Promise<TypeSignalement[]> {
  const types = await getCollection<FirestoreTypeSignalement>("type_signalements")
  return types
    .map((item) => ({
      id: toNumber(item.id) ?? 0,
      libelle: item.libelle ?? "",
    }))
    .filter((item) => item.id > 0 && item.libelle.trim() !== "")
}

export async function getFirestoreStatuts(): Promise<Statut[]> {
  const statuts = await getCollection<FirestoreStatut>("statuts")
  return statuts
    .map((item) => ({
      id: toNumber(item.id) ?? 0,
      libelle: item.libelle ?? "",
    }))
    .filter((item) => item.id > 0 && item.libelle.trim() !== "")
}

export async function getFirestoreEntreprises(): Promise<Entreprise[]> {
  const entreprises = await getCollection<FirestoreEntreprise>("entreprises")
  return entreprises
    .map((item) => ({
      id: toNumber(item.id) ?? 0,
      name: item.name ?? "",
      address: null,
      phone: null,
      active: null,
    }))
    .filter((item) => item.id > 0 && item.name.trim() !== "")
}

export async function getFirestoreSignalements(
  filters: { status?: string; type?: string } = {},
): Promise<SignalementMapDto[]> {
  const [
    signalements,
    statuts,
    entreprises,
    types,
  ] = await Promise.all([
    getCollection<FirestoreSignalement>("signalements"),
    getCollection<FirestoreStatut>("statuts"),
    getCollection<FirestoreEntreprise>("entreprises"),
    getCollection<FirestoreTypeSignalement>("type_signalements"),
  ])

  const statutsById = new Map<number, FirestoreStatut>()
  const statutsByLabel = new Map<string, FirestoreStatut>()
  statuts.forEach((item) => {
    const id = toNumber(item.id)
    if (id) {
      statutsById.set(id, item)
    }
    if (item.libelle) {
      statutsByLabel.set(normalizeLabel(item.libelle), item)
    }
  })

  const entreprisesById = new Map<number, FirestoreEntreprise>()
  const entreprisesByLabel = new Map<string, FirestoreEntreprise>()
  entreprises.forEach((item) => {
    const id = toNumber(item.id)
    if (id) {
      entreprisesById.set(id, item)
    }
    if (item.name) {
      entreprisesByLabel.set(normalizeLabel(item.name), item)
    }
  })

  const typesById = new Map<number, FirestoreTypeSignalement>()
  const typesByLabel = new Map<string, FirestoreTypeSignalement>()
  types.forEach((item) => {
    const id = toNumber(item.id)
    if (id) {
      typesById.set(id, item)
    }
    if (item.libelle) {
      typesByLabel.set(normalizeLabel(item.libelle), item)
    }
  })

  const list = mapFirestoreSignalementsRaw(
    signalements,
    statuts,
    entreprises,
    types,
  )

  const filtered = list.filter((item) => {
    if (filters.status && normalizeLabel(item.statut) !== normalizeLabel(filters.status)) {
      return false
    }
    if (
      filters.type &&
      normalizeLabel(item.typeSignalement) !== normalizeLabel(filters.type)
    ) {
      return false
    }
    return true
  })

  return filtered
}

export function mapFirestoreSignalementsRaw(
  signalements: FirestoreSignalement[],
  statuts: FirestoreStatut[] = [],
  entreprises: FirestoreEntreprise[] = [],
  types: FirestoreTypeSignalement[] = [],
): SignalementMapDto[] {
  const statutsById = new Map<number, FirestoreStatut>()
  const statutsByLabel = new Map<string, FirestoreStatut>()
  statuts.forEach((item) => {
    const id = toNumber(item.id)
    if (id) {
      statutsById.set(id, item)
    }
    if (item.libelle) {
      statutsByLabel.set(normalizeLabel(item.libelle), item)
    }
  })

  const entreprisesById = new Map<number, FirestoreEntreprise>()
  const entreprisesByLabel = new Map<string, FirestoreEntreprise>()
  entreprises.forEach((item) => {
    const id = toNumber(item.id)
    if (id) {
      entreprisesById.set(id, item)
    }
    if (item.name) {
      entreprisesByLabel.set(normalizeLabel(item.name), item)
    }
  })

  const typesById = new Map<number, FirestoreTypeSignalement>()
  const typesByLabel = new Map<string, FirestoreTypeSignalement>()
  types.forEach((item) => {
    const id = toNumber(item.id)
    if (id) {
      typesById.set(id, item)
    }
    if (item.libelle) {
      typesByLabel.set(normalizeLabel(item.libelle), item)
    }
  })

  return signalements
    .map((item) => {
      const id = toNumber(item.id)
      if (!id) {
        return null
      }
      const statut =
        item.statuts_id != null
          ? statutsById.get(toNumber(item.statuts_id) ?? -1)
          : item.status
          ? statutsByLabel.get(normalizeLabel(item.status))
          : null
      const entreprise =
        item.entreprise_id != null
          ? entreprisesById.get(toNumber(item.entreprise_id) ?? -1)
          : item.entreprise
          ? entreprisesByLabel.get(normalizeLabel(item.entreprise))
          : null
      const type =
        item.type_signalement_id != null
          ? typesById.get(toNumber(item.type_signalement_id) ?? -1)
          : item.title
          ? typesByLabel.get(normalizeLabel(item.title))
          : null
      return {
        id,
        latitude: toNumber(item.latitude),
        longitude: toNumber(item.longitude),
        date: toDateString(item.createdAt),
        description: item.description ?? null,
        surface: toNumber(item.surface),
        budget: toNumber(item.budget),
        statutsId: statut ? toNumber(statut.id) : null,
        statut: statut?.libelle ?? item.status ?? null,
        entrepriseId: entreprise ? toNumber(entreprise.id) : toNumber(item.entreprise_id),
        entreprise: entreprise?.name ?? item.entreprise ?? null,
        typeSignalementId: type ? toNumber(type.id) : null,
        typeSignalement: type?.libelle ?? item.title ?? null,
      } satisfies SignalementMapDto
    })
    .filter((item): item is SignalementMapDto => Boolean(item))
}

export async function getFirestoreSignalementsStats(
  filters: { status?: string; type?: string } = {},
): Promise<SignalementsStats> {
  const items = await getFirestoreSignalements(filters)
  const totalPoints = items.length
  const totalSurface = items
    .map((item) => item.surface)
    .filter((value): value is number => typeof value === "number")
    .reduce((sum, value) => sum + value, 0)
  const totalBudget = items
    .map((item) => item.budget)
    .filter((value): value is number => typeof value === "number")
    .reduce((sum, value) => sum + value, 0)
  const completed = items.filter((item) => isCompleted(item.statut)).length
  const advancementPercent = totalPoints === 0 ? 0 : (completed * 100) / totalPoints

  return {
    totalPoints,
    totalSurface,
    totalBudget,
    advancementPercent,
  }
}

export async function updateFirestoreSignalement(
  id: number,
  payload: {
    surface?: number | null
    budget?: number | null
    statutsId?: number | null
    typeSignalementId?: number | null
    entrepriseId?: number | null
    statutsLabel?: string | null
    typeLabel?: string | null
    entrepriseLabel?: string | null
  },
): Promise<SignalementMapDto> {
  const db = getFirestore(firebaseAuth.app)
  const collectionRef = collection(db, "signalements")
  const snapshot = await getDocs(query(collectionRef, where("id", "==", id)))
  if (snapshot.empty) {
    throw new Error("Signalement not found")
  }

  const docRef = snapshot.docs[0].ref
  const updates: Record<string, unknown> = {}
  if (payload.surface !== undefined) {
    updates.surface = payload.surface
  }
  if (payload.budget !== undefined) {
    updates.budget = payload.budget
  }
  if (payload.statutsLabel) {
    updates.status = payload.statutsLabel
  }
  if (payload.statutsId !== undefined) {
    updates.statuts_id = payload.statutsId
  }
  if (payload.typeLabel) {
    updates.title = payload.typeLabel
  }
  if (payload.typeSignalementId !== undefined) {
    updates.type_signalement_id = payload.typeSignalementId
  }
  if (payload.entrepriseLabel) {
    updates.entreprise = payload.entrepriseLabel
  }
  if (payload.entrepriseId !== undefined) {
    updates.entreprise_id = payload.entrepriseId
  }
  updates.updatedAt = new Date()

  await updateDoc(docRef, updates)

  const updatedSnap = await getDoc(docRef)
  const updated = updatedSnap.data() as FirestoreSignalement | undefined
  if (!updated) {
    throw new Error("Signalement not found")
  }

  return {
    id,
    latitude: toNumber(updated.latitude),
    longitude: toNumber(updated.longitude),
    date: toDateString(updated.createdAt),
    description: updated.description ?? null,
    surface: toNumber(updated.surface),
    budget: toNumber(updated.budget),
    statutsId: toNumber(updated.statuts_id),
    statut: updated.status ?? null,
    entrepriseId: toNumber(updated.entreprise_id),
    entreprise: updated.entreprise ?? null,
    typeSignalementId: toNumber(updated.type_signalement_id),
    typeSignalement: updated.title ?? null,
  }
}
