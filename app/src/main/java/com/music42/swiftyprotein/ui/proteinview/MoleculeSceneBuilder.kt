package com.music42.swiftyprotein.ui.proteinview

import com.google.android.filament.Engine
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.BondOrder
import com.music42.swiftyprotein.data.model.Ligand
import com.music42.swiftyprotein.util.CpkColors
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.normalize
import dev.romainguy.kotlin.math.cross
import dev.romainguy.kotlin.math.dot
import androidx.compose.ui.graphics.Color
import io.github.sceneview.collision.Sphere as CollisionSphere
import io.github.sceneview.collision.Vector3 as CollisionVector3
import io.github.sceneview.geometries.Cylinder
import io.github.sceneview.geometries.Sphere
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.MeshNode
import io.github.sceneview.node.Node
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MoleculeSceneBuilder {

    private const val BALL_RADIUS = 0.35f
    private const val STICK_RADIUS = 0.08f
    private const val MULTI_STICK_RADIUS = 0.05f
    private const val DOUBLE_BOND_OFFSET = 0.12f
    private const val TRIPLE_BOND_OFFSET = 0.14f
    private const val SPHERE_STACKS = 16
    private const val SPHERE_SLICES = 16

    fun build(
        engine: Engine,
        materialLoader: MaterialLoader,
        ligand: Ligand,
        mode: VisualizationMode,
        highlightElement: String?,
        centerOffset: Float3
    ): Pair<Node, Map<MeshNode, Atom>> {
        val parentNode = Node(engine)
        val atomNodeMap = mutableMapOf<MeshNode, Atom>()
        val atomsForRender = ligand.atoms.filterNot {
            val e = it.element.uppercase().trim()
            e == "H" || e == "D"
        }.ifEmpty { ligand.atoms }
        val atomById = atomsForRender.associateBy { it.id }

        val centerX = atomsForRender.map { it.x }.average().toFloat()
        val centerY = atomsForRender.map { it.y }.average().toFloat()
        val centerZ = atomsForRender.map { it.z }.average().toFloat()

        val highlight = highlightElement?.uppercase()?.trim()?.takeIf { it.isNotEmpty() }

        if (mode != VisualizationMode.STICKS_ONLY && mode != VisualizationMode.WIREFRAME) {
            val radius = if (mode == VisualizationMode.SPACE_FILL)
                BALL_RADIUS * 2.5f else BALL_RADIUS

            for (atom in atomsForRender) {
                val base = CpkColors.getColor(atom.element)
                val color = when {
                    highlight == null -> base
                    atom.element.uppercase().trim() == highlight -> brighten(base, 1.25f)
                    else -> dim(base, 0.45f)
                }
                val sphere = Sphere.Builder()
                    .radius(radius)
                    .center(Position(0f, 0f, 0f))
                    .stacks(SPHERE_STACKS)
                    .slices(SPHERE_SLICES)
                    .build(engine)

                val material = materialLoader.createColorInstance(
                    color = color,
                    metallic = 0.0f,
                    roughness = 0.6f,
                    reflectance = 0.3f
                )

                val meshNode = MeshNode(
                    engine = engine,
                    primitiveType = sphere.primitiveType,
                    vertexBuffer = sphere.vertexBuffer,
                    indexBuffer = sphere.indexBuffer,
                    boundingBox = sphere.boundingBox,
                    materialInstance = material
                ).apply {
                    position = Position(
                        (atom.x - centerX) - centerOffset.x,
                        (atom.y - centerY) - centerOffset.y,
                        (atom.z - centerZ) - centerOffset.z
                    )
                    isTouchable = true
                    collisionShape = CollisionSphere(
                        radius * 1.5f,
                        CollisionVector3(0f, 0f, 0f)
                    )
                }

                parentNode.addChildNode(meshNode)
                atomNodeMap[meshNode] = atom
            }
        }

        if (mode != VisualizationMode.SPACE_FILL) {
            for (bond in ligand.bonds) {
                val atom1 = atomById[bond.atomId1] ?: continue
                val atom2 = atomById[bond.atomId2] ?: continue

                val pos1 = Float3(
                    (atom1.x - centerX) - centerOffset.x,
                    (atom1.y - centerY) - centerOffset.y,
                    (atom1.z - centerZ) - centerOffset.z
                )
                val pos2 = Float3(
                    (atom2.x - centerX) - centerOffset.x,
                    (atom2.y - centerY) - centerOffset.y,
                    (atom2.z - centerZ) - centerOffset.z
                )
                val diff = Float3(pos2.x - pos1.x, pos2.y - pos1.y, pos2.z - pos1.z)
                val length = sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z)
                if (length < 0.001f) continue

                val color1 = CpkColors.getColor(atom1.element)
                val color2 = CpkColors.getColor(atom2.element)

                val stickCount = if (mode == VisualizationMode.WIREFRAME) {
                    1
                } else {
                    when (bond.order) {
                        BondOrder.SINGLE -> 1
                        BondOrder.DOUBLE, BondOrder.AROMATIC -> 2
                        BondOrder.TRIPLE -> 3
                    }
                }

                val radius = when {
                    mode == VisualizationMode.WIREFRAME -> 0.02f
                    stickCount > 1 -> MULTI_STICK_RADIUS
                    else -> STICK_RADIUS
                }
                val offsets = computeOffsets(diff, stickCount)

                for (offset in offsets) {
                    val p1 = Float3(
                        pos1.x + offset.x, pos1.y + offset.y, pos1.z + offset.z
                    )
                    val p2 = Float3(
                        pos2.x + offset.x, pos2.y + offset.y, pos2.z + offset.z
                    )
                    val mid = Float3(
                        (p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f, (p1.z + p2.z) / 2f
                    )
                    val halfLen = length / 2f

                    addHalfCylinder(
                        engine, materialLoader, parentNode,
                        Float3((p1.x + mid.x) / 2f, (p1.y + mid.y) / 2f, (p1.z + mid.z) / 2f),
                        halfLen, radius, color1, diff
                    )
                    addHalfCylinder(
                        engine, materialLoader, parentNode,
                        Float3((mid.x + p2.x) / 2f, (mid.y + p2.y) / 2f, (mid.z + p2.z) / 2f),
                        halfLen, radius, color2, diff
                    )
                }
            }
        }

        return parentNode to atomNodeMap
    }

    private fun dim(color: Color, factor: Float): Color {
        val f = factor.coerceIn(0f, 1f)
        return Color(color.red * f, color.green * f, color.blue * f, 1f)
    }

    private fun brighten(color: Color, factor: Float): Color {
        val f = factor.coerceAtLeast(1f)
        return Color(
            (color.red * f).coerceIn(0f, 1f),
            (color.green * f).coerceIn(0f, 1f),
            (color.blue * f).coerceIn(0f, 1f),
            1f
        )
    }

    private fun computeOffsets(direction: Float3, count: Int): List<Float3> {
        if (count == 1) return listOf(Float3(0f, 0f, 0f))

        val perp = perpendicular(direction)
        return when (count) {
            2 -> {
                val off = DOUBLE_BOND_OFFSET
                listOf(
                    Float3(perp.x * off, perp.y * off, perp.z * off),
                    Float3(-perp.x * off, -perp.y * off, -perp.z * off)
                )
            }
            3 -> {
                val off = TRIPLE_BOND_OFFSET
                listOf(
                    Float3(0f, 0f, 0f),
                    Float3(perp.x * off, perp.y * off, perp.z * off),
                    Float3(-perp.x * off, -perp.y * off, -perp.z * off)
                )
            }
            else -> listOf(Float3(0f, 0f, 0f))
        }
    }

    private fun perpendicular(v: Float3): Float3 {
        val dir = normalize(v)
        val ref = if (abs(dir.y) < 0.9f) Float3(0f, 1f, 0f) else Float3(1f, 0f, 0f)
        return normalize(cross(dir, ref))
    }

    private fun addHalfCylinder(
        engine: Engine,
        materialLoader: MaterialLoader,
        parent: Node,
        center: Float3,
        height: Float,
        radius: Float,
        color: androidx.compose.ui.graphics.Color,
        direction: Float3
    ) {
        val cylinder = Cylinder.Builder()
            .radius(radius)
            .height(height)
            .center(Position(0f, 0f, 0f))
            .build(engine)

        val material = materialLoader.createColorInstance(
            color = color,
            metallic = 0.0f,
            roughness = 0.4f,
            reflectance = 0.5f
        )

        val meshNode = MeshNode(
            engine = engine,
            primitiveType = cylinder.primitiveType,
            vertexBuffer = cylinder.vertexBuffer,
            indexBuffer = cylinder.indexBuffer,
            boundingBox = cylinder.boundingBox,
            materialInstance = material
        ).apply {
            position = Position(center.x, center.y, center.z)
            quaternion = cylinderQuaternion(direction)
            isTouchable = false
        }

        parent.addChildNode(meshNode)
    }

    private fun cylinderQuaternion(direction: Float3): Quaternion {
        val dir = normalize(direction)
        val up = Float3(0f, 1f, 0f)

        val d = dot(up, dir)
        if (d > 0.9999f) return Quaternion()
        if (d < -0.9999f) return Quaternion(0f, 0f, 1f, 0f)

        val axis = normalize(cross(up, dir))
        val halfAngle = acos(d.coerceIn(-1f, 1f)) / 2f
        val s = sin(halfAngle)

        return Quaternion(axis.x * s, axis.y * s, axis.z * s, cos(halfAngle))
    }
}
