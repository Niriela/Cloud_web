"use client"

import * as React from "react"
import { GalleryVerticalEnd, Map } from "lucide-react"

import { Button } from "~/components/ui/button"
import { NavMain } from "~/components/nav-main"
import { NavProjects } from "~/components/nav-projects"
import { NavUser } from "~/components/nav-user"
import { TeamSwitcher } from "~/components/team-switcher"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "~/components/ui/sidebar"
import { syncFirebase } from "~/lib/api"
import { useAuth } from "~/components/auth/auth-provider"

// This is sample data.
const data = {
  user: {
    name: "shadcn",
    email: "m@example.com",
    avatar: "/avatars/shadcn.jpg",
  },
  teams: [
    {
      name: "Gestion des routes",
      logo: GalleryVerticalEnd,
      plan: "Projet",
    },
  ],
  navMain: [
    {
      title: "Navigation",
      url: "#",
      icon: Map,
      isActive: true,
      items: [
        {
          title: "Accueil",
          url: "/Visiteurs",
        },
        {
          title: "Gerer les signalements",
          url: "/manager",
        },
        {
          title: "Gerer les utilisateurs",
          url: "/admin",
        },
        {
          title: "API Swagger",
          url: "http://localhost:8080/swagger-ui/index.html",
          external: true,
        },
      ],
    },
  ],
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { isAuthenticated } = useAuth()
  const [syncMessage, setSyncMessage] = React.useState<string | null>(null)
  const [isSyncing, setIsSyncing] = React.useState(false)

  const handleSync = async () => {
    setIsSyncing(true)
    setSyncMessage(null)
    try {
      await syncFirebase()
      setSyncMessage("Synchronisation terminee.")
    } catch {
      setSyncMessage("Echec de la synchronisation.")
    } finally {
      setIsSyncing(false)
    }
  }

  const navItems = isAuthenticated
    ? data.navMain
    : [
        {
          title: "Navigation",
          url: "#",
          icon: Map,
          isActive: true,
          items: [
            {
              title: "Accueil",
              url: "/",
            },
          ],
        },
      ]

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <TeamSwitcher teams={data.teams} />
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={navItems} />
      </SidebarContent>
      <SidebarFooter>
        {isAuthenticated ? (
          <div className="p-2">
            <Button
              type="button"
              variant="outline"
              className="w-full"
              disabled={isSyncing}
              onClick={handleSync}
            >
              {isSyncing ? "Synchronisation..." : "Synchroniser"}
            </Button>
            {syncMessage ? (
              <p className="mt-2 text-xs text-muted-foreground">{syncMessage}</p>
            ) : null}
          </div>
        ) : null}
        <NavUser user={data.user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
