package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;

/** Port for importing external asset formats into the Spectra domain. */
public interface AssetImportPort {
  SpectraAssetDocument read(String content, String modelId) throws Exception;
}
