package it.driveos.launcher

import it.driveos.launcher.voice.VoiceCommand
import it.driveos.launcher.voice.VoiceCommands
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandsTest {
    @Test fun navigation() {
        assertEquals(VoiceCommand.GoHome, VoiceCommands.parse("portami a casa"))
        assertEquals(VoiceCommand.GoWork, VoiceCommands.parse("andiamo al lavoro"))
        assertEquals(VoiceCommand.Navigate("piazza della loggia brescia"), VoiceCommands.parse("Portami in piazza della Loggia Brescia"))
        assertEquals(VoiceCommand.Navigate("milano"), VoiceCommands.parse("naviga verso milano"))
        assertEquals(VoiceCommand.StopNav, VoiceCommands.parse("termina la navigazione"))
    }

    @Test fun radioAndMedia() {
        assertEquals(VoiceCommand.Station("deejay"), VoiceCommands.parse("metti radio deejay"))
        assertEquals(VoiceCommand.Frequency(101500), VoiceCommands.parse("sintonizza centouno punto cinque"))
        assertEquals(VoiceCommand.Radio, VoiceCommands.parse("radio"))
        assertEquals(VoiceCommand.Music, VoiceCommands.parse("metti la musica"))
        assertEquals(VoiceCommand.Next, VoiceCommands.parse("prossima canzone"))
        assertEquals(VoiceCommand.VolumeUp, VoiceCommands.parse("alza il volume"))
        assertEquals(VoiceCommand.Climate, VoiceCommands.parse("apri il clima"))
    }
}
