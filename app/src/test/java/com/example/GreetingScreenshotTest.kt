package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.MatchItem
import com.example.data.model.MatchStatus
import com.example.data.model.StreamItem
import com.example.ui.components.MatchCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleMatch = MatchItem(
      id = "test_match_1",
      status = MatchStatus.LIVE,
      category = "Football",
      matchTitle = "Real Madrid vs Barcelona",
      tournamentName = "El Clasico - La Liga",
      tournamentLogo = "",
      teamAName = "Real Madrid",
      teamAFlag = "",
      teamBName = "Barcelona",
      teamBFlag = "",
      startTimeRaw = "01/09/2026 11:00:00 PM",
      streams = listOf(
        StreamItem("STREAM TV + HD", "http://example.com/stream.m3u8", "http://example.com/stream.m3u8", emptyMap())
      ),
      isFavorite = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        MatchCard(
          match = sampleMatch,
          onCardClick = {},
          onToggleFavorite = {},
          onQuickWatchClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

