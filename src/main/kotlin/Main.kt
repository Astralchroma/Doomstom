import awt.DoomFrame
import awt.DoomWindowController
import doom.DoomMain
import doom.event_t
import doom.evtype_t
import g.Signals.ScanCode
import mochadoom.Engine
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import java.awt.image.BufferedImage
import kotlin.concurrent.thread

fun main(oldArguments: Array<String>) {
	// Inject extra arguments
	val newArguments = mutableListOf("-nosound", "-multiply", "1")
	newArguments.addAll(oldArguments)
	val arguments = newArguments.toTypedArray()

	val gameInstances = mutableMapOf<Player, Pair<DoomMain<*, *>, Thread>>()

	val minecraftServer = MinecraftServer.init()
	val globalEventHandler = MinecraftServer.getGlobalEventHandler()

	val dimensionType =
		DimensionType
			.builder(NamespaceID.from("minecraft:the_end"))
			.effects("minecraft:the_end")
			.build()

	MinecraftServer.getDimensionTypeManager().addDimension(dimensionType)

	val instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimensionType)

	var bossBar: BossBar? = null

	globalEventHandler.addListener(PlayerLoginEvent::class.java) {
		it.setSpawningInstance(instance)

		it.player.gameMode = GameMode.ADVENTURE
		it.player.isFlying = true
		it.player.flyingSpeed = 0.5f

		val engineConstructor = Engine::class.java.getDeclaredConstructor(Array<String>::class.java)
		engineConstructor.isAccessible = true

		val engine = engineConstructor.newInstance(arguments)

		val doomField = Engine::class.java.getDeclaredField("DOOM")
		doomField.isAccessible = true

		val doomFrameField = DoomWindowController::class.java.getDeclaredField("doomFrame")
		doomFrameField.isAccessible = true

		val doomFrame = doomFrameField.get(engine.windowController) as DoomFrame<*>
		doomFrame.isVisible = false

		val doom = doomField.get(engine) as DoomMain<*, *>

		gameInstances[it.player] = Pair(doom, thread { doom.setupLoop() })

		bossBar = null
	}

	val movement = arrayOf(0, 0)

	globalEventHandler.addListener(PlayerChangeHeldSlotEvent::class.java) {
		when (it.slot) {
			8.toByte() -> {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_ENTER))
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_ENTER))
			}
			7.toByte() -> {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_UP))
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_UP))
			}
			6.toByte() -> {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_DOWN))
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_DOWN))
			}
		}

		it.slot = 0
	}

	globalEventHandler.addListener(PlayerMoveEvent::class.java) {
		it.player.gameMode = GameMode.ADVENTURE
		it.player.isFlying = true
		it.player.flyingSpeed = 0.5f

		if (it.newPosition.z > 0) {
			if (movement[0] == -1) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_S))
				movement[0] = 0
			}

			if (movement[0] == 0) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_W))
				movement[0] = 1
			}
		} else if (it.newPosition.z < 0) {
			if (movement[0] == 1) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_W))
				movement[0] = 0
			}

			if (movement[0] == 0) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_S))
				movement[0] = -1
			}
		} else {
			if (movement[0] == 1) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_W))
				movement[0] = 0
			}

			if (movement[0] == -1) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_S))
				movement[0] = 0
			}
		}

		if (it.newPosition.x > 0) {
			if (movement[1] == -1) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_D))
				movement[1] = 0
			}

			if (movement[1] == 0) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_A))
				movement[1] = 1
			}
		} else if (it.newPosition.x < 0) {
			if (movement[1] == 1) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_A))
				movement[1] = 0
			}

			if (movement[1] == 0) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_D))
				movement[1] = -1
			}
		} else {
			if (movement[1] == 1) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_A))
				movement[1] = 0
			}

			if (movement[1] == -1) {
				gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_D))
				movement[1] = 0
			}
		}

		if (it.newPosition.y < 0) {
			gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_LCTRL))
			gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keydown, ScanCode.SC_SPACE))
		} else {
			gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_LCTRL))
			gameInstances[it.player]!!.first.PostEvent(event_t.keyevent_t(evtype_t.ev_keyup, ScanCode.SC_SPACE))
		}

		gameInstances[it.player]!!.first.PostEvent(event_t.mouseevent_t(evtype_t.ev_mouse, 0, it.newPosition.yaw.toInt() * 30, 0))

		it.player.teleport(Pos.ZERO)
	}

	globalEventHandler.addListener(PlayerDisconnectEvent::class.java) {
		try {
			val pair = gameInstances.remove(it.player) ?: return@addListener
			pair.second.stop()
		} catch (e: Exception) { e.printStackTrace() }
	}

	val bufferedImage = BufferedImage(320, 200, BufferedImage.TYPE_INT_RGB)

	globalEventHandler.addListener(ServerTickMonitorEvent::class.java) {
		for ((player, pair) in gameInstances) {
			val doomFrame = pair.first

			val resolutionMultiplier: Double = 0.75

			val width = ((doomFrame.graphicSystem.screenWidth) * resolutionMultiplier).toInt()
			val height = ((doomFrame.graphicSystem.screenHeight) * resolutionMultiplier).toInt()

			if (resolutionMultiplier != 1.0) {
				bufferedImage.graphics.drawImage(doomFrame.graphicSystem.screenImage.getScaledInstance(width, height, 0), 0, 0, null)
			} else {
				bufferedImage.graphics.drawImage(doomFrame.graphicSystem.screenImage, 0, 0, null)
			}

			val component = Component.text().font(Key.key("doomstom:font"))

			val mask = Array(width) { BooleanArray(height) { false } }
			var remaining = (width / 2) * height

			var colour: Int? = null
			var masterBuffer = ""
			var buffer = ""
			var cursorX = 0

			val xMask = BooleanArray(width) { false }

			while (remaining > 0) {
				x@for (x in 0 until width step 2) {
					if (xMask[x]) continue

					var unusedY = false

					for (y in 0 until height) {
						if (mask[x][y]) continue

						unusedY = true

						fun add() {
							val offset = x - cursorX
							if (offset != 0) {
								buffer += (0xE20B + offset).toChar()
								cursorX += offset
							}
							buffer += (y + 0xE000).toChar()
							cursorX += 2
							mask[x][y] = true
							remaining--
						}

						if (colour == null) {
							colour = bufferedImage.getRGB(x, y)
							add()
							continue@x
						}

						if (colour == bufferedImage.getRGB(x, y)) {
							add()
							continue@x
						}
					}

					if (!unusedY) xMask[x] = true
				}

				masterBuffer += buffer
				component.append(Component.text(buffer).color(TextColor.color(colour ?: break)))
				buffer = ""
				colour = null
			}

			val offset = width - cursorX
			if (offset != 0) {
				masterBuffer += (0xE20B + offset).toChar()
				component.append(Component.text((0xE20B + offset).toChar()))
			}

			val builtComponent = component.build()

			if (bossBar == null) {
				bossBar = BossBar.bossBar(builtComponent, 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)

				player.showBossBar(bossBar!!)
			}
			else bossBar?.name(builtComponent)
		}
	}

	minecraftServer.start("0.0.0.0", 25565)
}