// runServer -Pkubejstest copies this into run/server/kubejs/server_scripts. It changes a Wonders data map, a recipe
// and a tag from a script, checks each on the live server, logs TWKJS lines and stops the server.
const DeviceRules = Java.loadClass('com.thaumicwonders.data.DeviceRules')
const Registries = Java.loadClass('net.minecraft.core.registries.Registries')
const ResourceKey = Java.loadClass('net.minecraft.resources.ResourceKey')
const Identifier = Java.loadClass('net.minecraft.resources.Identifier')
const TagKey = Java.loadClass('net.minecraft.tags.TagKey')

ServerEvents.generateData('after_mods', event => {
  event.json('thaumaturgicwonders:data_maps/item/catalyst', {
    values: {
      'thaumaturgicwonders:alchemist_stone': { flux_roll: 7, sparkle: 'thaumaturge:ordo' },
      'minecraft:stick': { flux_roll: 3, sparkle: 'thaumaturge:ordo' }
    }
  })
})

ServerEvents.recipes(event => {
  event.remove({ id: 'thaumaturgicwonders:quicksilver_bucket' })
  event.shaped('thaumaturgicwonders:quicksilver_bucket', [' R ', 'RBR', ' R '], {
    R: 'minecraft:redstone',
    B: 'minecraft:bucket'
  }).id('twkjs:quicksilver_bucket')
})

ServerEvents.tags('item', event => {
  event.add('thaumaturgicwonders:accretion_inputs', 'minecraft:diamond')
})

let fails = 0
function check(ok, what) {
  if (!ok) fails++
  console.info('TWKJS ' + (ok ? 'PASS ' : 'FAIL ') + what)
}

const BuiltInRegistries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries')
function catalyst(id) {
  let item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id))
  return BuiltInRegistries.ITEM.wrapAsHolder(item).getData(DeviceRules.CATALYST)
}

function recipe(server, id) {
  return server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse(id))).isPresent()
}

ServerEvents.loaded(event => {
  try {
    let stone = catalyst('thaumaturgicwonders:alchemist_stone')
    check(stone != null && stone.fluxChance() == 7, 'a script changes the alchemist stone flux roll (' + (stone == null ? 'none' : stone.fluxChance()) + ')')
    let stick = catalyst('minecraft:stick')
    check(stick != null && stick.fluxChance() == 3, 'a script makes the stick a catalyst')
    let transmuter = catalyst('thaumaturgicwonders:transmuter_stone')
    check(transmuter != null && transmuter.fluxChance() == 33, 'the shipped transmuter stone entry still applies')

    let server = event.server
    check(!recipe(server, 'thaumaturgicwonders:quicksilver_bucket'), 'the shipped quicksilver bucket recipe is removed')
    check(recipe(server, 'twkjs:quicksilver_bucket'), 'the script quicksilver bucket recipe is added')
    check(recipe(server, 'thaumaturgicwonders:quicksilver_bucket_deconstruct'), 'other Wonders recipes are untouched')

    let tag = TagKey.create(Registries.ITEM, Identifier.parse('thaumaturgicwonders:accretion_inputs'))
    check(Item.of('minecraft:diamond')['is(net.minecraft.tags.TagKey)'](tag), 'a script adds diamond to accretion inputs')
    check(Item.of('thaumaturgicwonders:primordial_grain')['is(net.minecraft.tags.TagKey)'](tag), 'the shipped accretion input is kept')
  } catch (e) {
    check(false, 'script threw: ' + e)
  }
  console.info('TWKJS RESULT ' + (fails == 0 ? 'PASS' : 'FAIL ' + fails))
  event.server.halt(false)
})
