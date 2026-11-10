import { useQuery } from "@tanstack/react-query";
import {
  Clock,
  FileText,
  GitCompare,
  Inbox,
  PercentCircle,
  Users,
} from "lucide-react";
import { StatCard } from "@/components/common/StatCard";
import { DataTable } from "@/components/common/DataTable";
import { BarraProporcion } from "@/components/common/BarraProporcion";
import { BadgeEstado, EncabezadoSeccion, Vacio } from "./comunes";
import { gestionQueries, type AnaliticaSemillero } from "@/lib/api/gestion";

type DemandaConvocatoria = AnaliticaSemillero["demandaPorConvocatoria"][number];

const MAX_HABILIDADES = 8;

/**
 * Pestaña "Indicadores" de la gestión (Servicio 3 para líderes y tutores). Se consulta solo
 * al abrir la pestaña: el cálculo recorre todos los datos del semillero y de la universidad.
 */
export function IndicadoresSemillero({ idSemillero }: { idSemillero: number }) {
  const { data, isPending, isError } = useQuery(
    gestionQueries.analitica(idSemillero),
  );

  if (isPending) return <Vacio>Calculando indicadores…</Vacio>;
  if (isError) return <Vacio>No se pudieron calcular los indicadores.</Vacio>;

  return (
    <div className="space-y-10">
      <section>
        <EncabezadoSeccion
          titulo="Indicadores del semillero"
          descripcion="Calculados solo con los datos de este semillero. Donde aplica, se muestra el valor de toda la universidad para comparar."
        />
        <ResumenIndicadores datos={data} />
      </section>

      <div className="grid gap-8 lg:grid-cols-2">
        <section>
          <EncabezadoSeccion
            titulo="Embudo de postulaciones"
            descripcion="En qué estado están todas las postulaciones que ha recibido el semillero."
          />
          <EmbudoPostulaciones datos={data} />
        </section>
        <section>
          <EncabezadoSeccion
            titulo="Habilidades de los admitidos"
            descripcion="Habilidades más frecuentes en las postulaciones aceptadas."
          />
          <HabilidadesAdmitidos datos={data} />
        </section>
      </div>

      <section>
        <EncabezadoSeccion
          titulo="Demanda por convocatoria"
          descripcion="Postulaciones recibidas por cada cupo ofertado. Una demanda alta indica que conviene abrir más cupos o ser más selectivo."
        />
        <DataTable<DemandaConvocatoria>
          rows={data.demandaPorConvocatoria}
          getRowKey={(fila) => fila.idConvocatoria}
          emptyMessage="El semillero aún no ha abierto convocatorias."
          columns={[
            {
              header: "Convocatoria",
              cell: (fila) => (
                <span className="font-medium">{fila.titulo}</span>
              ),
            },
            {
              header: "Estado",
              cell: (fila) => <BadgeEstado nombre={fila.estado} />,
            },
            {
              header: "Cupos disponibles",
              align: "right",
              cell: (fila) =>
                `${fila.cuposDisponibles} de ${fila.cuposTotales}`,
            },
            {
              header: "Postulaciones",
              align: "right",
              cell: (fila) => fila.postulacionesRecibidas,
            },
            {
              header: "Demanda",
              align: "right",
              cell: (fila) => `${fila.tasaDemanda.toFixed(2)} : 1`,
            },
          ]}
        />
      </section>

      <section>
        <EncabezadoSeccion
          titulo="Reconocimientos por entidad"
          descripcion="Quién ha otorgado los reconocimientos del semillero."
        />
        <ReconocimientosPorEntidad datos={data} />
      </section>
    </div>
  );
}

function ResumenIndicadores({ datos }: { datos: AnaliticaSemillero }) {
  const referencia = datos.referenciaInstitucional;
  const ofertas = datos.tasaRetencion;
  const conversion = datos.conversionProyectos;

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <StatCard
        label="Integrantes activos"
        value={datos.integrantesActivos}
        hint={`${datos.integrantesRetirados} retirado(s). ${datos.interdisciplinariedad.porcentajeInterdisciplinariedad.toFixed(1)} % de las vinculaciones son de otro programa (universidad: ${referencia.porcentajeInterdisciplinariedad.toFixed(1)} %).`}
        icon={Users}
      />
      <StatCard
        label="Postulaciones en proceso"
        value={datos.postulacionesActivas}
        hint={`Pendientes de revisión o con oferta sin responder. ${datos.totalPostulaciones} recibidas en total.`}
        icon={Inbox}
      />
      <StatCard
        label="Demanda"
        value={datos.tasaDemanda.toFixed(2)}
        unit=": 1"
        hint={`Postulaciones por cupo ofertado. Universidad: ${referencia.tasaDemandaPromedio.toFixed(2)} : 1.`}
        icon={PercentCircle}
      />
      <StatCard
        label="Ofertas aceptadas"
        value={ofertas.porcentajeRetencion.toFixed(1)}
        unit="%"
        hint={
          ofertas.totalOfertasProcesadas > 0
            ? `${ofertas.ofertasAceptadas} de ${ofertas.totalOfertasProcesadas} ofertas resueltas. Universidad: ${referencia.porcentajeRetencion.toFixed(1)} %.`
            : "Aún no hay ofertas resueltas."
        }
        icon={GitCompare}
      />
      <StatCard
        label="Respuesta a ofertas"
        value={datos.tiempoRespuesta.promedioHoras.toFixed(1)}
        unit="h"
        hint={
          datos.tiempoRespuesta.totalPostulacionesEvaluadas > 0
            ? `Promedio entre la oferta y la decisión del estudiante. Universidad: ${referencia.promedioHorasRespuesta.toFixed(1)} h.`
            : "Ningún estudiante ha respondido una oferta todavía."
        }
        icon={Clock}
      />
      <StatCard
        label="Publicaciones por proyecto"
        value={conversion.promedioPublicacionesPorProyecto.toFixed(2)}
        hint={`${conversion.totalPublicaciones} publicaciones en ${conversion.totalProyectos} proyectos (universidad: ${referencia.promedioPublicacionesPorProyecto.toFixed(2)}). ${datos.participacionEstudiantil.porcentajeParticipacionEstudiantil.toFixed(1)} % de las autorías son de estudiantes.`}
        icon={FileText}
      />
    </div>
  );
}

function EmbudoPostulaciones({ datos }: { datos: AnaliticaSemillero }) {
  if (datos.totalPostulaciones === 0) {
    return <Vacio>El semillero aún no ha recibido postulaciones.</Vacio>;
  }
  return (
    <div className="panel p-5">
      <ul className="space-y-4">
        {Object.entries(datos.postulacionesPorEstado).map(
          ([estado, cantidad]) => (
            <BarraProporcion
              key={estado}
              etiqueta={estado}
              cantidad={cantidad}
              total={datos.totalPostulaciones}
            />
          ),
        )}
      </ul>
    </div>
  );
}

function HabilidadesAdmitidos({ datos }: { datos: AnaliticaSemillero }) {
  const habilidades = datos.habilidadesEmergentes.slice(0, MAX_HABILIDADES);
  if (habilidades.length === 0) {
    return <Vacio>Aún no hay postulaciones aceptadas con habilidades.</Vacio>;
  }
  const total = datos.habilidadesEmergentes.reduce(
    (suma, habilidad) => suma + habilidad.frecuencia,
    0,
  );
  return (
    <div className="panel p-5">
      <ul className="space-y-4">
        {habilidades.map((habilidad) => (
          <BarraProporcion
            key={habilidad.habilidad}
            etiqueta={habilidad.habilidad}
            cantidad={habilidad.frecuencia}
            total={total}
          />
        ))}
      </ul>
    </div>
  );
}

function ReconocimientosPorEntidad({ datos }: { datos: AnaliticaSemillero }) {
  const { totalReconocimientos, distribucionPorEntidad } =
    datos.impactoReconocimientos;
  if (totalReconocimientos === 0) {
    return <Vacio>El semillero aún no registra reconocimientos.</Vacio>;
  }
  return (
    <div className="panel max-w-3xl p-5">
      <ul className="space-y-4">
        {Object.entries(distribucionPorEntidad)
          .sort(([, a], [, b]) => b - a)
          .map(([entidad, cantidad]) => (
            <BarraProporcion
              key={entidad}
              etiqueta={entidad}
              cantidad={cantidad}
              total={totalReconocimientos}
            />
          ))}
      </ul>
    </div>
  );
}
