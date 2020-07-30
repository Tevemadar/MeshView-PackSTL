# MeshView-PackSTL

Pack STL meshes for MeshView.

Usage:

`java PackSTL input.stl output.png`

Only a narrow variant of STL is supported, triangle lists in the form

    solid ascii
     facet normal 0.6147089327063285 -0.2524115075206019 -0.7472759590152914
      outer loop
       vertex 251.23466106092917 438.74877098946274 377.7241471225742
       vertex 251.07548958333334 441.8446759259259 376.5474918981481
       vertex 251.72955555555555 442.02847222222226 377.02344444444446
      endloop
     endfacet
     ...
    endsolid

"Coincidentally" [MeshGen](https://www.nitrc.org/projects/meshgen/) happens to generate this format, from NIfTI volumes.

An example STL file is provided,

`java PackSTL "043-pineal gland.stl" 43.png`

is expected to generate the same 54653 bytes which are available at https://github.com/Tevemadar/meshview-demo/blob/master/WHS_SD_rat_atlas_v2/43.png