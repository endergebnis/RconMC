package gg.endergebnis.rconmc.verify;

import java.util.Optional;

/**
 * Austauschbare Verifizierung: Eine spaetere Bedrock/Floodgate-Variante kann dieselbe Schnittstelle
 * implementieren, ohne die restliche Logik zu aendern.
 */
public interface AccountVerifier {

    /**
     * @return den Account, oder leer, wenn der Name nicht existiert
     * @throws VerificationException wenn die Pruefung selbst fehlschlaegt
     */
    Optional<VerifiedAccount> verify(String name);
}
