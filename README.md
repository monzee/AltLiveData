# Alt LiveData
A simple lifecycle-aware reactive system that is meant to be a replacement
for the `LiveData` component of the Android Arch Components library.

- Provides an almost drop-in replacement for `MutableLiveData<T>`.
- Allows `ViewModel`s with live fields to be testable in local JUnit tests
  without needing a special rule or runner.
- Provides an `Either L R` interface (called `Try<T>` here) that can be extended
  using transformers/decorators and can be unwrapped synchronously.
- Provides an abstraction that extends the `Either L R` idea with another branch
  indicating that the computation has not completed yet.

## Why
- problems with livedata

## Overview
lorem ipsum

### `Try<T>`
lorem ipsum

### `Live<T>`
lorem ipsum

### `Loader<T>`
lorem ipsum

### `Task<I, O>`
lorem ipsum

## Installation
This library is published at JCenter.

For Android app/library modules:
```gradle
dependencies {
    api "ph.codeia.altlive:android:$version"
}
```
For plain Java modules or if you only need `Try<T>`:
```gradle
dependencies {
    api "ph.codeia.altlive:core:$version"
}
```
Scroll up to the version badge to find out the latest version available and
replace the version number accordingly.

## License
```
MIT License

Copyright (c) 2018 Mon Zafra

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

