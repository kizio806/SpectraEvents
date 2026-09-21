(function () {
  var pluginAction;

  Plugin.register('spectraevents_exporter', {
    title: 'SpectraEvents Exporter',
    author: 'SpectraEvents Team',
    description: 'Exports a signed Generic Model project as a .spectra.zip bundle for SpectraEvents.',
    icon: 'archive',
    version: '2.0.0',
    variant: 'both',
    onload() {
      pluginAction = new Action('export_spectra_bundle', {
        name: 'Export Spectra Bundle',
        description: 'Export a signed .spectra.zip bundle',
        icon: 'archive',
        click: exportSpectraBundle
      });
      MenuBar.addAction(pluginAction, 'file.export');
    },
    onunload() {
      pluginAction.delete();
    }
  });

  async function exportSpectraBundle() {
    if (!Format || Format.id !== 'free') {
      Blockbench.showMessageBox({
        title: 'SpectraEvents export',
        message: 'SpectraEvents supports only Blockbench Generic Model projects. Create or convert the project before exporting.'
      });
      return;
    }

    try {
      const crypto = require('crypto');
      const Buffer = require('buffer').Buffer;
      const model = JSON.parse(Codecs.project.compile());
      const modelId = safeModelId(Project.name);
      const bundle = new JSZip();
      const checksums = {};
      const texturePaths = [];

      if (!Array.isArray(model.textures) || model.textures.length === 0) {
        throw new Error('The project must contain at least one embedded PNG texture.');
      }

      model.meta = model.meta || {};
      model.meta.model_format = 'free';

      model.textures.forEach(function (texture, index) {
        if (!texture || typeof texture.source !== 'string' || !texture.source.startsWith('data:image/png;base64,')) {
          throw new Error('Every texture must be embedded PNG data before export. Use Blockbench’s Embed Texture action.');
        }
        const path = 'textures/texture_' + index + '.png';
        const base64 = texture.source.substring('data:image/png;base64,'.length);
        const bytes = Buffer.from(base64, 'base64');
        if (bytes.length === 0) {
          throw new Error('Texture ' + (texture.name || index) + ' has no PNG content.');
        }
        texture.source = path;
        texturePaths.push(path);
        checksums[path] = sha256(crypto, bytes);
        bundle.file(path, bytes);
      });

      const modelJson = JSON.stringify(model, null, 2);
      const modelBytes = Buffer.from(modelJson, 'utf8');
      checksums['model.bbmodel'] = sha256(crypto, modelBytes);
      bundle.file('model.bbmodel', modelBytes);
      bundle.file(
        'manifest.json',
        JSON.stringify(
          {
            schemaVersion: 1,
            modelId: modelId,
            model: 'model.bbmodel',
            textures: texturePaths,
            sha256: checksums
          },
          null,
          2
        )
      );

      const content = await bundle.generateAsync({ type: 'blob' });
      Blockbench.export({
        type: 'Spectra Bundle',
        extensions: ['spectra.zip'],
        savetype: 'zip',
        name: modelId + '.spectra.zip',
        content: content
      });
      Blockbench.showQuickMessage('Exported ' + modelId + '.spectra.zip', 2500);
    } catch (error) {
      Blockbench.showMessageBox({
        title: 'SpectraEvents export failed',
        message: error && error.message ? error.message : String(error)
      });
    }
  }

  function safeModelId(name) {
    let modelId = String(name || 'model')
      .toLowerCase()
      .replace(/[^a-z0-9_-]+/g, '-')
      .replace(/^[-_]+|[-_]+$/g, '')
      .slice(0, 64);
    if (!/^[a-z0-9]/.test(modelId)) {
      modelId = 'model';
    }
    return modelId;
  }

  function sha256(crypto, bytes) {
    return crypto.createHash('sha256').update(bytes).digest('hex');
  }
})();
