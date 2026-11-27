# 1.21.1 build

NeoForge 21.1 build of the same mod, kept beside the 26.1.2 sources on branch `backport/1.21.1`.
Needs `libs/thaumaturge-1.21.1-NeoForge-1.1.0.jar` (not committed).

Differences from 26.1.2:
- Item models use `models/item` overrides with predicates from `client/TWItemProperties` (no `items/` definitions).
- The creative essentia jar item uses its static block model.
- Armour textures come from `getArmorTexture` (no `equipment/` assets).
- Recipe ingredients use the object form (`{"item"}`, `{"tag"}`, `"type": "neoforge:components"`).
- JEI pages register through the 19.x API.
