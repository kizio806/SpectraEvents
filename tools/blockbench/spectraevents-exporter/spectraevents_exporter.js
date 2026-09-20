(function() {
    var pluginAction;

    Plugin.register('spectraevents_exporter', {
        title: 'SpectraEvents Exporter',
        author: 'SpectraEvents Team',
        description: 'Exports the current model to a .spectra.zip bundle for SpectraEvents.',
        icon: 'archive',
        version: '1.0.0',
        variant: 'both',
        onload() {
            pluginAction = new Action('export_spectra_bundle', {
                name: 'Export Spectra Bundle',
                description: 'Export as .spectra.zip bundle',
                icon: 'archive',
                click: function() {
                    exportSpectraBundle();
                }
            });
            MenuBar.addAction(pluginAction, 'file.export');
        },
        onunload() {
            pluginAction.delete();
        }
    });

    function exportSpectraBundle() {
        if (Format.id !== 'free' && Format.id !== 'modded_entity') {
            Blockbench.showQuickMessage('Warning: spectra bundle works best with Generic Model format.', 2000);
        }

        const zip = new JSZip();

        // 1. Generate Manifest
        const manifest = {
            schemaVersion: 1,
            generator: 'SpectraEvents Blockbench Exporter',
            generatorVersion: '1.0.0',
            modelId: Project.name || 'unnamed_model',
            sourceFormat: Format.id,
            sourceFormatVersion: Format.format_version || 'unknown',
            createdWithBlockbenchVersion: Blockbench.version,
            files: [],
            checksums: {} // Checksums are handled by the server side on import for security
        };

        // 2. Add Model JSON
        // We use the built-in JSON generation for bbmodel files
        const modelJson = Codecs.project.compile();
        zip.file('model.json', modelJson);
        manifest.files.push('model.json');

        // 3. Add Textures
        const textureFolder = zip.folder('textures');
        Texture.all.forEach(tex => {
            if (tex.source && tex.source.startsWith('data:image')) {
                // base64 image
                const base64Data = tex.source.split(',')[1];
                const texName = tex.name.endsWith('.png') ? tex.name : tex.name + '.png';
                textureFolder.file(texName, base64Data, { base64: true });
                manifest.files.push('textures/' + texName);
            }
        });

        // Add manifest to root
        zip.file('manifest.json', JSON.stringify(manifest, null, 2));

        // Generate Zip
        zip.generateAsync({ type: 'blob' }).then(content => {
            Blockbench.export({
                type: 'Spectra Bundle',
                extensions: ['spectra.zip'],
                savetype: 'zip',
                name: (Project.name || 'model') + '.spectra.zip',
                content: content
            });
        });
    }
})();
