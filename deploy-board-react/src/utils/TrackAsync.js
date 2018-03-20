import {observable, action, reaction} from 'mobx'

let pendingAsync = observable(0)

// export a debounced, computed value instead of pendingAsync > 0, to avoid notifying clients
// when pendingAsync changes from 1 to 2, 3, etc
let ASYNC_PENDING = observable(false)

// monitor and debounced value of pendingAsync to a simple true/false
reaction(() => pendingAsync.get() > 0, res => ASYNC_PENDING.set(res))

const beginAsync = action(() => pendingAsync.set(pendingAsync.get() + 1))
const endAsync = action(() => pendingAsync.set(pendingAsync.get() - 1))

function trackAsync(...args) {
  if (args.length === 1) {
    return wrapFunction(...args)
  } else {
    return wrapMethod(...args)
  }
}

function wrapFunction(fn) {
  return async (...args) => {
    beginAsync()
    try {
      return await fn(...args)
    } catch (err) {
      throw err
    } finally {
      endAsync()
    }
  }
}

function wrapMethod(target, key, descriptor) {
  let fn = descriptor.value

  if (typeof fn !== 'function') {
    throw new Error(`@decorator can only be applied to methods not: ${typeof fn}`)
  }

  // In IE11 calling Object.defineProperty has a side-effect of evaluating the
  // getter for the property which is being replaced. This causes infinite
  // recursion and an "Out of stack space" error.
  let definingProperty = false

  return {
    configurable: true,
    get() {
      if (definingProperty || this === target.prototype) {
        //|| this.hasOwnProperty(key)) {
        return fn
      }
      let wrapped = wrapFunction(fn.bind(this))
      definingProperty = true
      Object.defineProperty(this, key, {
        configurable: true,
        get() {
          return wrapped
        }
      })
      definingProperty = false
      return wrapped
    }
  }
}

export {trackAsync, beginAsync, endAsync, ASYNC_PENDING}
