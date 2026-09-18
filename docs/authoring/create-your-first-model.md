# Create Your First SpectraEvents Model

This tutorial guides you through creating, importing, building, and spawning a simple 3D model using SpectraEvents.

## 1. Create the Model in Blockbench
1. Open Blockbench and start a **Generic Model** project.
2. Under the **Outliner**, create a new group (folder) and name it `root`.
3. Inside `root`, create another group named `cube`.
4. Inside `cube`, create a single block/cube.
5. Add a texture named `example.png` and paint your cube.
6. Make sure to map your UVs to the texture (Box UV is recommended).

## 2. Add an Animation
1. Switch to the **Animate** tab in Blockbench.
2. Create a new animation named `spin`. Set the loop mode to **Loop**.
3. Select the `cube` group.
4. Add rotation keyframes:
   - At `0.0s`: Rotation `[0, 0, 0]`
   - At `1.0s`: Rotation `[0, 360, 0]`
5. This creates a continuous 360° spin over 1 second.

## 3. Export
1. Ensure the `spectra_exporter.js` plugin is installed (see [Blockbench Guide](blockbench.md)).
2. Go to **File** -> **Export** -> **Export Spectra Bundle**.
3. Save the file as `example_cube.spectra.zip`.

## 4. Import & Validate
1. Place `example_cube.spectra.zip` in your server directory or designated upload folder.
2. In the server console or in-game, run:
   `/event assets import example_cube.spectra.zip`
3. Validate the model definition:
   `/event assets validate example_cube`

## 5. Build Resource Pack
Once imported, you must compile the asset pipeline to generate the resource pack:
`/event assets build`

This builds the `pack.mcmeta`, models, and textures into the final resource pack state.

## 6. Spawn and Animate
Log into your server and run:
`/event model spawn example_cube`

To play the animation:
`/event animation play example_cube spin`

Congratulations! You have successfully created, imported, and animated a SpectraEvents model.
