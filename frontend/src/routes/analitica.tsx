import { createFileRoute } from "@tanstack/react-router";
import { useSuspenseQuery } from "@tanstack/react-query";
import { Award, Clock, FileText, GitCompare, PercentCircle, Users } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/common/PageHeader";
import { StatCard } from "@/components/common/StatCard";
import { DataTable, type Columna } from "@/components/common/DataTable";
import { analiticaQuery } from "@/lib/api/queries";
import type { DemandaSemillero, DistribucionReconocimientos, HabilidadEmergente } from "@/types/api";

export const Route = createFileRoute("/analitica")({
  head: () => ({
    meta: [
      { title: "Analítica institucional | Agora UdeA" },
      {
        name: "description",
        content:
          "Indicadores de impacto institucional de los semilleros UdeA: demanda por cupos, productividad científica, reconocimientos y habilidades emergentes.",
      },
      { property: "og:title", content: "Analítica institucional | Agora UdeA" },
      {
        property: "og:description",
        content:
          "Tableros de KPIs para la Vicerrectoría de Investigación de la Universidad de Antioquia.",
      },
    ],
  }),
  loader: async ({ context }) => {
    await context.queryClient.ensureQueryData(analiticaQuery());
  },
  component: AnaliticaPage,
});

function AnaliticaPage() {
  const { data } = useSuspenseQuery(analiticaQuery());
  const { kpis, demandaPorSemillero, reconocimientos, habilidadesEmergentes } = data;
  const totalReconocimientos = reconocimientos.reduce((suma, item) => suma + item.total, 0);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Vicerrectoría de Investigación"
        title="Analítica institucional"
        description="Indicadores de atracción, eficiencia de vinculación, productividad científica y capacidades técnicas derivados de los catálogos normalizados del sistema."
      />

      <section aria-labelledby="eficiencia" className="mb-10">
        <h2 id="eficiencia" className="mb-3 font-display text-sm font-semibold text-foreground">
          Atracción y eficiencia de vinculación
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <StatCard
            label="Tasa de demanda promedio"
            value={kpis.tasaDemandaPromedio.toFixed(2)}
            hint="Postulaciones recibidas por cada cupo ofertado."
            icon={PercentCircle}
          />
          <StatCard
            label="Respuesta del estudiante"
            value={kpis.tiempoRespuestaEstudianteHoras.toFixed(1)}
            unit="h"
            hint="Tiempo promedio entre la pre-aprobación y la decisión final."
            icon={Clock}
          />
          <StatCard
            label="Rechazo de ofertas"
            value={kpis.tasaRechazoOfertas.toFixed(1)}
            unit="%"
            hint="Ofertas declinadas o expiradas frente a las pre-aprobadas."
            icon={GitCompare}
          />
        </div>
      </section>

      <section aria-labelledby="productividad" className="mb-10">
        <h2 id="productividad" className="mb-3 font-display text-sm font-semibold text-foreground">
          Productividad científica y demografía
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <StatCard
            label="Conversión proyecto-publicación"
            value={kpis.indiceConversionProyectoPublicacion.toFixed(2)}
            hint="Publicaciones promedio por proyecto finalizado."
            icon={FileText}
          />
          <StatCard
            label="Participación estudiantil en autorías"
            value={kpis.participacionEstudiantilAutorias.toFixed(1)}
            unit="%"
            hint="Autores que son estudiantes frente al total de autorías registradas."
            icon={Users}
          />
          <StatCard
            label="Índice de interdisciplinariedad"
            value={kpis.indiceInterdisciplinariedad.toFixed(1)}
            unit="%"
            hint="Vinculaciones cruzadas entre programas académicos distintos."
            icon={Award}
          />
        </div>
      </section>

      <section aria-labelledby="demanda" className="mb-10">
        <h2 id="demanda" className="mb-3 font-display text-sm font-semibold text-foreground">
          Presión sobre cupos por semillero
        </h2>
        <DataTable<DemandaSemillero>
          rows={[...demandaPorSemillero].sort((a, b) => b.tasaDemanda - a.tasaDemanda)}
          getRowKey={(row) => row.idSemillero}
          columns={columnasDemanda}
        />
      </section>

      <div className="grid gap-8 lg:grid-cols-2">
        <section aria-labelledby="reconocimientos">
          <h2
            id="reconocimientos"
            className="mb-3 font-display text-sm font-semibold text-foreground"
          >
            Distribución del impacto por ámbito
          </h2>
          <div className="panel p-5">
            <ul className="space-y-4">
              {reconocimientos.map((item) => (
                <BarraReconocimiento
                  key={item.ambito}
                  item={item}
                  total={totalReconocimientos || 1}
                />
              ))}
            </ul>
            <p className="mt-5 border-t border-border pt-4 text-xs text-muted-foreground">
              Total de reconocimientos registrados: {totalReconocimientos}
            </p>
          </div>
        </section>

        <section aria-labelledby="habilidades">
          <h2 id="habilidades" className="mb-3 font-display text-sm font-semibold text-foreground">
            Mapa de habilidades emergentes
          </h2>
          <DataTable<HabilidadEmergente>
            rows={habilidadesEmergentes}
            getRowKey={(row) => row.nombre}
            columns={[
              { header: "Habilidad", cell: (row) => <span className="font-medium">{row.nombre}</span> },
              {
                header: "Postulaciones aceptadas",
                align: "right",
                cell: (row) => row.totalAceptadas,
              },
            ]}
          />
        </section>
      </div>
    </AppShell>
  );
}

const etiquetasAmbito: Record<DistribucionReconocimientos["ambito"], string> = {
  LOCAL: "Local / interno",
  NACIONAL: "Nacional",
  INTERNACIONAL: "Internacional",
};

function BarraReconocimiento({
  item,
  total,
}: {
  item: DistribucionReconocimientos;
  total: number;
}) {
  const porcentaje = (item.total / total) * 100;
  return (
    <li>
      <div className="flex items-baseline justify-between text-sm">
        <span className="font-medium text-foreground">{etiquetasAmbito[item.ambito]}</span>
        <span className="tabular-nums text-muted-foreground">
          {item.total} · {porcentaje.toFixed(1)}%
        </span>
      </div>
      <div className="mt-1.5 h-2 w-full overflow-hidden rounded-sm bg-secondary">
        <div className="h-full bg-primary" style={{ width: `${porcentaje}%` }} />
      </div>
    </li>
  );
}

const columnasDemanda: Columna<DemandaSemillero>[] = [
  { header: "Semillero", cell: (row) => <span className="font-medium">{row.nombre}</span> },
  { header: "Cupos ofertados", align: "right", cell: (row) => row.cuposOfertados },
  { header: "Postulaciones", align: "right", cell: (row) => row.postulacionesRecibidas },
  {
    header: "Tasa de demanda",
    align: "right",
    cell: (row) => `${row.tasaDemanda.toFixed(2)} : 1`,
  },
];
