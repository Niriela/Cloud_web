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

// This is sample data.
const data = {
  user: {
    name: "shadcn",
    email: "m@example.com",
    avatar: "/avatars/shadcn.jpg",
  },
  teams: [
    {
      name: "Acme Inc",
      logo: GalleryVerticalEnd,
      plan: "Enterprise",
    },
    {
      name: "Acme Corp.",
      logo: Map,
      plan: "Startup",
    },
    {
      name: "Evil Corp.",
      logo: GalleryVerticalEnd,
      plan: "Free",
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
      ],
    },
  ],
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
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

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <TeamSwitcher teams={data.teams} />
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
      </SidebarContent>
      <SidebarFooter>
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
        <NavUser user={data.user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
