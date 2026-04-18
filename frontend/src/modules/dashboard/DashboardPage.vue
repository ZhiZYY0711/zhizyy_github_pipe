<script setup lang="ts">
import KpiStrip from '../../components/shell/KpiStrip.vue'
import LeftRail from '../../components/shell/LeftRail.vue'
import MapPanel from '../../components/shell/MapPanel.vue'
import SideStack from '../../components/shell/SideStack.vue'
import TopBar from '../../components/shell/TopBar.vue'
import { dashboardPageModel } from '../../data/mockShell'
</script>

<template>
  <section class="dashboard-page-module">
    <div class="dashboard-page-module__frame">
      <TopBar
        :system-eyebrow="dashboardPageModel.systemEyebrow"
        :system-name="dashboardPageModel.systemName"
        :nav-items="dashboardPageModel.navItems"
        :status-items="dashboardPageModel.statusItems"
      />

      <div class="dashboard-page-module__workspace">
        <LeftRail :cards="dashboardPageModel.leftRailCards" />

        <section class="dashboard-page-module__main">
          <KpiStrip :items="dashboardPageModel.kpis" />

          <div class="dashboard-page-module__board">
            <MapPanel :panel="dashboardPageModel.mapPanel" />
            <SideStack :panels="dashboardPageModel.sidePanels" />
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page-module {
  position: relative;
  min-block-size: 100dvh;
  block-size: 100dvh;
  overflow: hidden;
  padding: var(--space-5);
  background-image:
    linear-gradient(rgba(96, 121, 139, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(96, 121, 139, 0.06) 1px, transparent 1px);
  background-size: 28px 28px;
}

.dashboard-page-module::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 16% 14%, rgba(110, 202, 212, 0.06), transparent 24%),
    radial-gradient(circle at 84% 18%, rgba(231, 104, 45, 0.08), transparent 20%),
    radial-gradient(circle at 58% 78%, rgba(110, 202, 212, 0.04), transparent 18%);
  opacity: 0.9;
}

.dashboard-page-module__frame {
  position: relative;
  z-index: 1;
  block-size: 100%;
  display: grid;
  grid-template-rows: 72px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--color-line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.015), rgba(255, 255, 255, 0.004)),
    var(--color-bg-elevated);
  box-shadow: var(--shadow-shell);
}

.dashboard-page-module__workspace {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  min-block-size: 0;
  overflow: hidden;
}

.dashboard-page-module__main {
  min-width: 0;
  min-height: 0;
  padding: var(--space-4);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: var(--space-3);
  overflow: hidden;
}

.dashboard-page-module__board {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(320px, 0.84fr);
  gap: var(--space-3);
  overflow: hidden;
}

.dashboard-page-module :deep(.left-rail),
.dashboard-page-module :deep(.side-stack),
.dashboard-page-module :deep(.map-panel),
.dashboard-page-module :deep(.panel),
.dashboard-page-module :deep(.map-body) {
  min-height: 0;
}

.dashboard-page-module :deep(.left-rail) {
  block-size: 100%;
}

.dashboard-page-module :deep(.side-stack) {
  block-size: 100%;
  grid-template-rows: repeat(2, minmax(0, 1fr));
}

@media (max-width: 1439px) {
  .dashboard-page-module__workspace {
    grid-template-columns: 236px minmax(0, 1fr);
  }

  .dashboard-page-module__board {
    grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.88fr);
  }
}

@media (max-width: 1179px) {
  .dashboard-page-module {
    block-size: auto;
    min-block-size: 100dvh;
    padding: var(--space-4);
    overflow: auto;
  }

  .dashboard-page-module__frame {
    block-size: auto;
    grid-template-rows: auto minmax(0, 1fr);
  }

  .dashboard-page-module__workspace,
  .dashboard-page-module__board {
    grid-template-columns: 1fr;
  }

  .dashboard-page-module__main {
    overflow: visible;
  }

  .dashboard-page-module :deep(.left-rail) {
    border-right: 0;
    border-bottom: 1px solid var(--color-line);
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-page-module :deep(.side-stack) {
    grid-template-rows: none;
  }
}

@media (max-width: 719px) {
  .dashboard-page-module :deep(.left-rail) {
    grid-template-columns: 1fr;
  }
}
</style>
