package com.udea.agora_backend.controller;

import com.udea.agora_backend.dto.request.ElementoCatalogoRequestDTO;
import com.udea.agora_backend.dto.response.CategoriaEstadoResponseDTO;
import com.udea.agora_backend.dto.response.EntidadOtorganteResponseDTO;
import com.udea.agora_backend.dto.response.FacultadResponseDTO;
import com.udea.agora_backend.dto.response.OauthProveedorResponseDTO;
import com.udea.agora_backend.dto.response.ProgramaAcademicoResponseDTO;
import com.udea.agora_backend.dto.response.RolAutorResponseDTO;
import com.udea.agora_backend.dto.response.RolEstudianteProyectoResponseDTO;
import com.udea.agora_backend.dto.response.RolResponseDTO;
import com.udea.agora_backend.dto.response.TipoReconocimientoResponseDTO;
import com.udea.agora_backend.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
@Tag(name = "Catálogos Generales", description = "Endpoints de consulta de diccionarios normalizados y catálogos institucionales")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @Operation(summary = "Consultar catálogo de facultades")
    @ApiResponse(responseCode = "200", description = "Lista de facultades obtenida exitosamente")
    @GetMapping("/facultades")
    public ResponseEntity<List<FacultadResponseDTO>> obtenerFacultades() {
        return ResponseEntity.ok(catalogoService.obtenerFacultades());
    }

    @Operation(summary = "Consultar catálogo de programas académicos")
    @ApiResponse(responseCode = "200", description = "Lista de programas académicos obtenida exitosamente")
    @GetMapping("/programas-academicos")
    public ResponseEntity<List<ProgramaAcademicoResponseDTO>> obtenerProgramasAcademicos() {
        return ResponseEntity.ok(catalogoService.obtenerProgramasAcademicos());
    }

    @Operation(summary = "Consultar programas académicos filtrados por facultad")
    @ApiResponse(responseCode = "200", description = "Lista de programas académicos de la facultad obtenida exitosamente")
    @GetMapping("/programas-academicos/facultad/{idFacultad}")
    public ResponseEntity<List<ProgramaAcademicoResponseDTO>> obtenerProgramasPorFacultad(@PathVariable Integer idFacultad) {
        return ResponseEntity.ok(catalogoService.obtenerProgramasPorFacultad(idFacultad));
    }

    @Operation(summary = "Consultar catálogo de roles de usuario (Estudiante, Profesor, Administrador)")
    @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente")
    @GetMapping("/roles")
    public ResponseEntity<List<RolResponseDTO>> obtenerRoles() {
        return ResponseEntity.ok(catalogoService.obtenerRoles());
    }

    @Operation(summary = "Consultar catálogo de roles de autoría para publicaciones")
    @ApiResponse(responseCode = "200", description = "Lista de roles de autor obtenida exitosamente")
    @GetMapping("/roles-autor")
    public ResponseEntity<List<RolAutorResponseDTO>> obtenerRolesAutor() {
        return ResponseEntity.ok(catalogoService.obtenerRolesAutor());
    }

    @Operation(summary = "Consultar catálogo de roles de estudiantes en proyectos de investigación")
    @ApiResponse(responseCode = "200", description = "Lista de roles de estudiante en proyecto obtenida exitosamente")
    @GetMapping("/roles-estudiante-proyecto")
    public ResponseEntity<List<RolEstudianteProyectoResponseDTO>> obtenerRolesEstudianteProyecto() {
        return ResponseEntity.ok(catalogoService.obtenerRolesEstudianteProyecto());
    }

    @Operation(summary = "Consultar catálogo de tipos de reconocimiento")
    @ApiResponse(responseCode = "200", description = "Lista de tipos de reconocimiento obtenida exitosamente")
    @GetMapping("/tipos-reconocimiento")
    public ResponseEntity<List<TipoReconocimientoResponseDTO>> obtenerTiposReconocimiento() {
        return ResponseEntity.ok(catalogoService.obtenerTiposReconocimiento());
    }

    @Operation(summary = "Consultar catálogo de entidades otorgantes de reconocimientos")
    @ApiResponse(responseCode = "200", description = "Lista de entidades otorgantes obtenida exitosamente")
    @GetMapping("/entidades-otorgantes")
    public ResponseEntity<List<EntidadOtorganteResponseDTO>> obtenerEntidadesOtorgantes() {
        return ResponseEntity.ok(catalogoService.obtenerEntidadesOtorgantes());
    }

    @Operation(summary = "Consultar catálogo de categorías de estado")
    @ApiResponse(responseCode = "200", description = "Lista de categorías de estado obtenida exitosamente")
    @GetMapping("/categorias-estado")
    public ResponseEntity<List<CategoriaEstadoResponseDTO>> obtenerCategoriasEstado() {
        return ResponseEntity.ok(catalogoService.obtenerCategoriasEstado());
    }

    @Operation(summary = "Consultar catálogo de proveedores OAuth soportados")
    @ApiResponse(responseCode = "200", description = "Lista de proveedores OAuth obtenida exitosamente")
    @GetMapping("/oauth-proveedores")
    public ResponseEntity<List<OauthProveedorResponseDTO>> obtenerProveedoresOAuth() {
        return ResponseEntity.ok(catalogoService.obtenerProveedoresOAuth());
    }

    // Alta directa por el administrador (regla en SecurityConfig). Estudiantes y profesores
    // usan /api/solicitudes-catalogo.

    @Operation(summary = "Agregar un tipo de reconocimiento (administrador)")
    @PostMapping("/tipos-reconocimiento")
    public ResponseEntity<TipoReconocimientoResponseDTO> crearTipoReconocimiento(
            @Valid @RequestBody ElementoCatalogoRequestDTO request) {
        return new ResponseEntity<>(catalogoService.crearTipoReconocimiento(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Agregar una entidad otorgante (administrador)")
    @PostMapping("/entidades-otorgantes")
    public ResponseEntity<EntidadOtorganteResponseDTO> crearEntidadOtorgante(
            @Valid @RequestBody ElementoCatalogoRequestDTO request) {
        return new ResponseEntity<>(catalogoService.crearEntidadOtorgante(request), HttpStatus.CREATED);
    }
}
