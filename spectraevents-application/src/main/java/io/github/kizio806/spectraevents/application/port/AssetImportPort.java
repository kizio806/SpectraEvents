package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.nio.file.Path;

/** Port for importing external asset formats into the Spectra domain. */
public interface AssetImportPort {
  SpectraAssetDocument read(Path sourceFile) throws Exception;
}
